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

/**
 * Description of a user profile.
 * @author Peter Kharchenko
 * @version $Revision$
 */

public class UserProfile {
    protected String pName;
    protected String struct_ss_name;
    protected String theme_ss_name;
    protected String description;
    protected boolean system=false;

    public UserProfile(String name,String struct_ss, String theme_ss, String desc) {
	pName=name; struct_ss_name=struct_ss; theme_ss_name=theme_ss; description=desc;
    }

    public String getProfileName() { return pName; }
    public String getProfileDescription() { return description; }
    public String getStructureStylesheetName() { return struct_ss_name; }
    public String getThemeStylesheetName() { return theme_ss_name; }
    public boolean isSystemProfile(){return system; }

    public void setProfileName(String name) { pName=name; }
    public void setStructureStylesheetName(String ss_name) { struct_ss_name=ss_name; }
    public void setThemeStylesheetName(String ss_name) { theme_ss_name=ss_name; }
    public void setProfileDescription(String desc) { description=desc; }
    public void setSystemProfile(boolean s) { system=s; }
}
