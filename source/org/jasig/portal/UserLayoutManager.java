/**
 * Copyright (c) 2000 The JA-SIG Collaborative.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the JA-SIG Collaborative
 *    (http://www.jasig.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JA-SIG COLLABORATIVE "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JA-SIG COLLABORATIVE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.jasig.portal;

import org.jasig.portal.security.IPerson;

import java.sql.*;
import org.w3c.dom.*;

import org.apache.xalan.xpath.*;
import org.apache.xalan.xslt.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

/**
 * UserLayoutManager participates in all operations associated with the
 * user layout and user preferences.
 * @author Peter Kharchenko
 * @version $Revision$
 */

public class UserLayoutManager {
  private Document uLayoutXML;

  // this one will contain user Layout XML with attrubutes for the
  // first (structure) transformation
  private Document argumentedUserLayoutXML;

  private MediaManager mediaM;

  private UserPreferences up;
  private UserPreferences complete_up;

    private boolean unmapped_user_agent=false;

    private IPerson person;

  /**
   * Constructor does the following
   *  1. Read layout.properties
   *  2. read userLayout from the database
   *  @param the servlet request object
   *  @param person object
   */
  public UserLayoutManager (HttpServletRequest req, IPerson person)
  {
    String fs = System.getProperty ("file.separator");
    String propertiesDir = GenericPortalBean.getPortalBaseDir () + "properties" + fs;
    MediaManager mediaM = new MediaManager (propertiesDir + "media.properties", propertiesDir + "mime.properties", propertiesDir + "serializer.properties");

    uLayoutXML = null;

    try
    {
	this.person=person;
        // read uLayoutXML
        if (this.person == null) {
	    // determine the default user
	    this.person=new org.jasig.portal.security.provider.PersonImpl();
	    this.person.setID("guest");
	}

        // load user preferences
        IUserPreferencesDB updb=new UserPreferencesDBImpl();

	// determine user profile
	String userAgent=req.getHeader("user-Agent");
	UserProfile upl=updb.getUserProfile(this.person.getID(),userAgent);
	if(upl!=null) {
	    IUserLayoutDB uldb = new UserLayoutDBImpl();
	    uLayoutXML = uldb.getUserLayout(this.person.getID(),upl.getProfileName());
	    if(uLayoutXML==null) Logger.log(Logger.ERROR,"UserLayoutManager::UserLayoutManager() : unable to retreive userLayout for user=\""+this.person.getID()+"\", profile=\""+upl.getProfileName()+"\".");
	    this.setCurrentUserPreferences(updb.getUserPreferences(this.person.getID(),upl));
	} else {
	    // there is no user-defined mapping for this particular browser.
	    // user should be redirected to a browser-registration page.
	    unmapped_user_agent=true;
	    Logger.log(Logger.DEBUG,"UserLayoutManager::UserLayoutManager() : unable to find a profile for user \""+this.person.getID()+"\" and userAgent=\""+userAgent+"\".");
	};
    }
    catch (Exception e)
    {
      Logger.log(Logger.ERROR,e);
    }
  }

    public IPerson getPerson() { 
	return person; 
    }

    public boolean userAgentUnmapped() { return unmapped_user_agent; }

    public void synchUserPreferencesWithLayout(UserPreferences someup) {

        StructureStylesheetUserPreferences fsup=someup.getStructureStylesheetUserPreferences();
        ThemeStylesheetUserPreferences ssup=someup.getThemeStylesheetUserPreferences();

        // make a list of channels in the XML Layout
        NodeList channelNodes=uLayoutXML.getElementsByTagName("channel");
        HashSet channelSet=new HashSet();
        for(int i=0;i<channelNodes.getLength();i++) {
            Element el=(Element) channelNodes.item(i);
            if(el!=null) {
                String chID=el.getAttribute("ID");
                if(!fsup.hasChannel(chID)) {
                    fsup.addChannel(chID);
                    //		    Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : StructureStylesheetUserPreferences were missing a channel="+chID);
                }
                if(!ssup.hasChannel(chID)) {
                    ssup.addChannel(chID);
                    //		    Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : ThemeStylesheetUserPreferences were missing a channel="+chID);
                }
                channelSet.add(chID);
            }

        }

        // make a list of categories in the XML Layout
        NodeList folderNodes=uLayoutXML.getElementsByTagName("folder");
        HashSet folderSet=new HashSet();
        for(int i=0;i<folderNodes.getLength();i++) {
            Element el=(Element) folderNodes.item(i);
            if(el!=null) {
                String caID=el.getAttribute("ID");
                if(!fsup.hasFolder(caID)) {
                    fsup.addFolder(caID);
                    //		    Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : StructureStylesheetUserPreferences were missing a folder="+caID);
                }
                folderSet.add(caID);
            }
        }

        // cross check
        for(Enumeration e=fsup.getChannels();e.hasMoreElements();) {
            String chID=(String)e.nextElement();
            if(!channelSet.contains(chID)) {
                fsup.removeChannel(chID);
                //		Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : StructureStylesheetUserPreferences had a non-existent channel="+chID);
            }
        }

        for(Enumeration e=fsup.getCategories();e.hasMoreElements();) {
            String caID=(String)e.nextElement();
            if(!folderSet.contains(caID)) {
                fsup.removeFolder(caID);
                //		Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : StructureStylesheetUserPreferences had a non-existent folder="+caID);
            }
        }

        for(Enumeration e=ssup.getChannels();e.hasMoreElements();) {
            String chID=(String)e.nextElement();
            if(!channelSet.contains(chID)) {
                ssup.removeChannel(chID);
                //		Logger.log(Logger.DEBUG,"UserLayoutManager::synchUserPreferencesWithLayout() : ThemeStylesheetUserPreferences had a non-existent channel="+chID);
            }
        }
        someup.setStructureStylesheetUserPreferences(fsup);
        someup.setThemeStylesheetUserPreferences(ssup);

    }

    public UserPreferences getCompleteCurrentUserPreferences() {
        return complete_up;
    }

    public void setCurrentUserPreferences(UserPreferences current_up) {
        if(current_up!=null) up=current_up;
        // load stylesheet description files and fix user preferences
        ICoreStylesheetDescriptionDB csddb=new CoreStylesheetDescriptionDBImpl();

        // clean up
        StructureStylesheetUserPreferences fsup=up.getStructureStylesheetUserPreferences();
        StructureStylesheetDescription fssd=csddb.getStructureStylesheetDescription(fsup.getStylesheetName());
        if(fssd==null) {
            // assign a default stylesheet instead

        } else {
            fsup.synchronizeWithDescription(fssd);
        }

        ThemeStylesheetUserPreferences ssup=up.getThemeStylesheetUserPreferences();
        ThemeStylesheetDescription sssd=csddb.getThemeStylesheetDescription(ssup.getStylesheetName());
        if(sssd==null) {
            // assign a default stylesheet instead
        } else {
            ssup.synchronizeWithDescription(sssd);
        }


        // in case something was reset to default
        up.setStructureStylesheetUserPreferences(fsup);
        up.setThemeStylesheetUserPreferences(ssup);


        // now generate "filled-out copies"
        complete_up=new UserPreferences(up);
        // syncronize up with layout
        synchUserPreferencesWithLayout(complete_up);
        StructureStylesheetUserPreferences complete_fsup=complete_up.getStructureStylesheetUserPreferences();
        ThemeStylesheetUserPreferences complete_ssup=complete_up.getThemeStylesheetUserPreferences();
        complete_fsup.completeWithDescriptionInformation(fssd);
        complete_ssup.completeWithDescriptionInformation(sssd);
        complete_up.setStructureStylesheetUserPreferences(complete_fsup);
        complete_up.setThemeStylesheetUserPreferences(complete_ssup);

        // complete user preferences are used to:
        //  1. fill out userLayoutXML with attributes required for the first transform
        //  2. contruct a filter that will fill out attributes required for the second transform

        //	argumentedUserLayoutXML=(Document) uLayoutXML.cloneNode(true);
        argumentedUserLayoutXML= UtilitiesBean.cloneDocument((org.apache.xerces.dom.DocumentImpl) uLayoutXML);


        // deal with folder attributes first
        NodeList folderElements=argumentedUserLayoutXML.getElementsByTagName("folder");
        if(folderElements==null)
            Logger.log(Logger.DEBUG,"UserLayoutManager::setCurrentUserPreferences() : empty list of folder elements obtained!");
        List cl=complete_fsup.getFolderAttributeNames();
        for(int j=0;j<cl.size();j++) {
            for(int i=folderElements.getLength()-1;i>=0;i--) {
                Element folderElement=(Element) folderElements.item(i);
                folderElement.setAttribute((String) cl.get(j),complete_fsup.getFolderAttributeValue(folderElement.getAttribute("ID"),(String) cl.get(j)));
                //		Logger.log(Logger.DEBUG,"UserLayoutManager::setCurrentUserPreferences() : added attribute "+(String) cl.get(j)+"="+complete_fsup.getFolderAttributeValue(folderElement.getAttribute("ID"),(String) cl.get(j))+" for a folder "+folderElement.getAttribute("ID"));
            }
        }
        // channel attributes
        NodeList channelElements=argumentedUserLayoutXML.getElementsByTagName("channel");
        if(channelElements==null)
            Logger.log(Logger.DEBUG,"UserLayoutManager::setCurrentUserPreferences() : empty list of channel elements obtained!");
        List chl=complete_fsup.getChannelAttributeNames();
        for(int j=0;j<chl.size();j++) {
            for(int i=channelElements.getLength()-1;i>=0;i--) {
                Element channelElement=(Element) channelElements.item(i);
                channelElement.setAttribute((String) chl.get(j),complete_fsup.getChannelAttributeValue(channelElement.getAttribute("ID"),(String) chl.get(j)));
                //		Logger.log(Logger.DEBUG,"UserLayoutManager::setCurrentUserPreferences() : added attribute "+(String) chl.get(j)+"="+complete_fsup.getChannelAttributeValue(channelElement.getAttribute("ID"),(String) chl.get(j))+" for a channel "+channelElement.getAttribute("ID"));
            }
        }
    }

    /*
     * Resets both user layout and user preferences.
     * Note that if any of the two are "null", old values will be used.
     */
    public void setNewUserLayoutAndUserPreferences(Document newLayout,UserPreferences newPreferences) {
        if(newLayout!=null) {
            uLayoutXML=newLayout;
            IUserLayoutDB uldb=new UserLayoutDBImpl();
            uldb.setUserLayout(person.getID(),up.getProfile().getProfileName(),uLayoutXML);
        }
        if(newPreferences!=null) {
            this.setCurrentUserPreferences(newPreferences);
            IUserPreferencesDB updb=new UserPreferencesDBImpl();
            updb.putUserPreferences(person.getID(),up);
        }

    }


    public Document getUserLayoutCopy() {
        return UtilitiesBean.cloneDocument((org.apache.xerces.dom.DocumentImpl) uLayoutXML);
    }

    public UserPreferences getUserPreferencesCopy() {
        return new UserPreferences(up);
    }

    public UserProfile getCurrentProfile() {
	return up.getProfile();
    }

  public Node getNode (String elementID)
  {
    return argumentedUserLayoutXML.getElementById (elementID);
  }

  public Node getCleanNode (String elementID)
  {
    return uLayoutXML.getElementById (elementID);
  }

  public Node getRoot ()
  {
    return argumentedUserLayoutXML;
  }


  public void minimizeChannel (String str_ID)
  {
    Element channel = argumentedUserLayoutXML.getElementById (str_ID);

    if (channel != null)
    {
      if (channel.getAttribute ("minimized").equals ("false"))
        channel.setAttribute ("minimized", "true");
      else
        channel.setAttribute ("minimized", "false");
    }
    else
      Logger.log (Logger.ERROR, "UserLayoutManager::minimizeChannel() : unable to find a channel with ID=" + str_ID);
  }

    // helper function that allows to determine the name of a channel or
    // folder in the current user layout given their ID.
    public String getNodeName(String nodeID) {
	Element node=argumentedUserLayoutXML.getElementById(nodeID);
	if(node!=null) {
	    return node.getAttribute("name");
	} else return null;
    }
	

  public void removeChannel (String str_ID)
  {
      // warning .. the channel should also be removed from uLayoutXML
    Element channel = argumentedUserLayoutXML.getElementById (str_ID);

    if (channel != null)
    {
      Node parent = channel.getParentNode ();

      if (parent != null)
        parent.removeChild (channel);
      else
        Logger.log (Logger.ERROR, "UserLayoutManager::removeChannel() : attempt to remove a root node !");
    }
    else Logger.log (Logger.ERROR, "UserLayoutManager::removeChannel() : unable to find a channel with ID="+str_ID);
  }

    private Element getChildByTagName(Node node,String tagName) {
        if(node==null) return null;
        NodeList children=node.getChildNodes();
        for(int i=children.getLength()-1;i>=0;i--) {
            Node child=children.item(i);
            if(child.getNodeType()==Node.ELEMENT_NODE) {
                Element el=(Element) child;
                if((el.getTagName()).equals(tagName))
                    return el;
            }
        }
        return null;
    }


}
