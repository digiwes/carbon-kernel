/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.osgi.deployment;

import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.deployment.api.DeploymentService;
import org.wso2.carbon.deployment.exception.CarbonDeploymentException;
import org.wso2.carbon.deployment.spi.Deployer;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

/**
 * Carbon Deployment Engine OSGi Test case.
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CarbonDeploymentEngineOSGiTest {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private DeploymentService deploymentService;

    private static String artifactPath;


    static {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        Path testResourceDir = Paths.get(basedir, "src", "test", "resources");
        artifactPath = Paths.get(testResourceDir.toString(), "carbon-repo", "text-files", "sample1.txt").toString();
    }

    @Test
    public void testRegisterDeployer() {
        ServiceRegistration serviceRegistration = bundleContext.registerService(Deployer.class.getName(),
                new CustomDeployer(), null);
        ServiceReference reference = bundleContext.getServiceReference(Deployer.class.getName());
        Assert.assertNotNull(reference, "Custom Deployer Service Reference is null");
        CustomDeployer deployer = (CustomDeployer) bundleContext.getService(reference);
        Assert.assertNotNull(deployer, "Custom Deployer Service is null");
        serviceRegistration.unregister();
        reference = bundleContext.getServiceReference(Deployer.class.getName());
        Assert.assertNull(reference, "Custom Deployer Service Reference should be unregistered and null");

        //register faulty deployers
        CustomDeployer deployer1 = new CustomDeployer();
        deployer1.setArtifactType(null);
        bundleContext.registerService(Deployer.class.getName(), deployer1, null);

        CustomDeployer deployer2 = new CustomDeployer();
        deployer2.setLocation(null);
        bundleContext.registerService(Deployer.class.getName(), deployer2, null);
    }

    @Test(dependsOnMethods = {"testRegisterDeployer"})
    public void testDeploymentService() throws CarbonDeploymentException {
        Assert.assertNotNull(deploymentService);
        CustomDeployer customDeployer = new CustomDeployer();
        bundleContext.registerService(Deployer.class.getName(), customDeployer, null);
        //undeploy
        try {
            deploymentService.undeploy(artifactPath, customDeployer.getArtifactType());
        } catch (CarbonDeploymentException e) {
            Assert.assertEquals(e.getMessage(), "Cannot find artifact with key : " + artifactPath + " to undeploy");
        }
        //deploy
        deploymentService.deploy(artifactPath, customDeployer.getArtifactType());

        //redeploy - this does not do anything for the moment.
        deploymentService.redeploy(artifactPath, customDeployer.getArtifactType());
    }
}