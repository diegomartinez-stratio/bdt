/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.qa.specs;

import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;


/**
 * Generic /etc/hosts management Specs.
 *
 * @see <a href="ETCHOSTSManagementSpec-annotations.html">/etc/hosts Management Steps &amp; Matching Regex</a>
 */
public class ETCHOSTSManagementSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public ETCHOSTSManagementSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * If there is no previous backup of /etc/hosts and we can get the lock, it will create it and add the new entry,
     * otherwise it will only add the new entry.
     *
     * @param hostname  hostname to be added to /etc/hosts
     * @param ip    ip address to be added to /etc/hosts
     * @param remote    whether we want to change /etc/hosts locally or in a remote system
     * @param sshConnectionId   previously saved ssh connection where to perform the action
     * @throws Exception
     */
    @Given("^I save host '(.+?)' with ip '(.+?)' in /etc/hosts( in the ssh connection)?( with id '(.+?)')?$")
    public void addEntryHostsFile(String hostname, String ip, String remote, String sshConnectionId) throws Exception {
        // Try to acquire lock
        commonspec.getETCHOSTSManagementUtils().acquireLock(remote, sshConnectionId, ip, hostname);
    }

    /**
     * If /etc/hosts backup file exists and we have the lock, restore it, otherwise do nothing
     *
     * @param remote    whether we want to restore /etc/hosts locally or in a remote system
     * @param sshConnectionId   previously saved ssh connection where to perform the action
     * @throws Exception
     */
    @Given("^I restore /etc/hosts( in the ssh connection)?( with id '(.+?)')?$")
    public void restoreHostsFile(String remote, String sshConnectionId) throws Exception {
        // Release lock
        commonspec.getETCHOSTSManagementUtils().releaseLock(remote, sshConnectionId);
    }

    /**
     * Obtain java PID process of current execution and save it in environment variable
     *
     * @param envVar    variable where to save current java PID
     */
    @Given("^I obtain java pid and save the value in environment variable '(.+?)'$")
    public void obtainJavaPID(String envVar) {
        String javaPID = commonspec.getETCHOSTSManagementUtils().obtainPID();
        ThreadProperty.set(envVar, javaPID);
    }

    @Given("^the host '(.+?)' is reachable in less than '(\\d+)' seconds$")
    public void isHostReachable(String host, Integer timeout) throws Exception {
        if (!commonspec.isHostReachable(host, timeout)) {
            throw new Exception("Host " + host + " was not reachable after " + timeout + " seconds");
        }
    }
}
