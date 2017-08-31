/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.util.resource;

import java.io.File;

import junit.framework.TestCase;

/** Unit test for {@link ResourceChangeWatcher}. */
public class ResourceChangeWatcherTest extends TestCase {
    
    /** A resource that exists. */
    private Resource existingResource;
    
    /** A resource that does not exist. */
    private Resource nonExistingResource;
    
    /** A resource that does not exist. */
    private Resource inaccessibleResource;
    
    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        existingResource = new FilesystemResource(new File(FilesystemResource.class.getResource(
                "/data/org/opensaml/util/resource/replacementFilterTest.txt").toURI()).getAbsolutePath());
        
        nonExistingResource = new FilesystemResource("/foo");
        
        inaccessibleResource = new HttpResource("http://shibfoo.example.com/foo.txt");
    }
    
    /**
     * Test that watcher construction succeeds when resource is successfully determined to exist.
     */
    public void testConstructorResourceExists() {
        try {
            assertTrue(existingResource.exists());
        } catch (ResourceException e) {
            fail("Bad control data");
        }
        
        try {
            ResourceChangeWatcher watcher = new ResourceChangeWatcher(existingResource, ResourceChangeWatcher.DEFAULT_POLL_FREQUENCY, 0);
        } catch (ResourceException e) {
            fail("Existing resource failed change watcher construction");
        }
    }
    
    /**
     * Test that watcher construction succeeds when resource is successfully determined to not exist.
     */
    public void testConstructorResourceNotExists() {
        try {
            assertFalse(nonExistingResource.exists());
        } catch (ResourceException e) {
            fail("Bad control data");
        }
        
        try {
            ResourceChangeWatcher watcher = new ResourceChangeWatcher(nonExistingResource, ResourceChangeWatcher.DEFAULT_POLL_FREQUENCY, 0);
        } catch (ResourceException e) {
            fail("Non-existing resource failed construction");
        }
    }
    
    /**
     * Test that watcher construction fails when resource is inaccessible (existence can not be determined)
     * and no retries are configured.
     */
    public void testConstructorResourceInaccessibleNoRetries() {
        try {
            inaccessibleResource.exists();
            fail("Bad control data");
        } catch (ResourceException e) {
            //should fail with exception
        }
        
        try {
            ResourceChangeWatcher watcher = new ResourceChangeWatcher(inaccessibleResource, ResourceChangeWatcher.DEFAULT_POLL_FREQUENCY, 0);
            fail("Inaccessible resource passed construction with 0 retries");
        } catch (ResourceException e) {
            //should fail with exception
        }
    }
    
    /**
     * Test that watcher construction succeeds when resource is inaccessible (existence can not be determined)
     * but a sufficient number of retries exists.
     */
    public void testConstructorResourceInaccessibleWithRetries() {
        try {
            inaccessibleResource.exists();
            fail("Bad control data");
        } catch (ResourceException e) {
            //should fail with exception
        }
        
        try {
            ResourceChangeWatcher watcher = new ResourceChangeWatcher(inaccessibleResource, ResourceChangeWatcher.DEFAULT_POLL_FREQUENCY, 2);
        } catch (ResourceException e) {
            fail("Inaccessible resource failed construction with 2 retries");
        }
    }

}
