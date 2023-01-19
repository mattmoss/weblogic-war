# weblogic-war

### Project Setup

This project was created with: `mn create-app demo.weblogic-war` using Micronaut v3.5.4.
The only changes that have been made are in the gradle file:
* Adding "war" and "application" plugins
* Changing micronaut.runtime to "jetty"
* Adding these dependencies:
  ```
  developmentOnly('io.micronaut.servlet:micronaut-http-server-jetty:3.3.3')
  implementation('io.micronaut.servlet:micronaut-servlet-engine:3.3.3')
  ```
* Setting java source and target compatibility to Java version 8

### Summary of Issue

Running `gradlew war` generates `build/libs/weblogic-war-0.1.war` which, when loaded onto
Oracle WebLogic 12c server, fails to start with the following exception:

```
weblogic.application.ModuleException: io.micronaut.context.exceptions.NoSuchBeanException: No bean of type [io.micronaut.context.event.ApplicationEventPublisher<io.micronaut.context.event.StartupEvent>] exists. Make sure the bean is not disabled by bean requirements (enable trace logging for 'io.micronaut.context.condition' to check) and if the bean is enabled then ensure the class is declared a bean and annotation processing is enabled (for Java and Kotlin the 'micronaut-inject-java' dependency should be configured as an annotation processor).:io.micronaut.context.exceptions.NoSuchBeanException:No bean of type [io.micronaut.context.event.ApplicationEventPublisher<io.micronaut.context.event.StartupEvent>] exists. Make sure the bean is not disabled by bean requirements (enable trace logging for 'io.micronaut.context.condition' to check) and if the bean is enabled then ensure the class is declared a bean and annotation processing is enabled (for Java and Kotlin the 'micronaut-inject-java' dependency should be configured as an annotation processor).
	at io.micronaut.context.DefaultBeanContext.resolveBeanRegistration(DefaultBeanContext.java:2784)
	at io.micronaut.context.DefaultBeanContext.getBean(DefaultBeanContext.java:1596)
	at io.micronaut.context.DefaultBeanContext.getBean(DefaultBeanContext.java:865)
	at io.micronaut.context.BeanLocator.getBean(BeanLocator.java:94)
	at io.micronaut.context.DefaultBeanContext.publishEvent(DefaultBeanContext.java:1682)
```

The `ApplicationEventPublisher` bean cannot be created because `io.micronaut.context.event.ApplicationEventPublisherFactory`
is not available. The latter is usually created via `io.micronaut.core.io.service.SoftServiceLoader` in
`DefaultServiceCollector.compute` starting with these lines:

```
    final String path = "META-INF/micronaut/" + serviceName;
    final Enumeration<URL> micronautResources = classLoader.getResources(path);
```

Running under WebLogic, the URLs at this point look like `zip:/full/path/to/some-1.2.3.jar!/META-INF/micronaut/io.micronaut.inject.BeanDefinitionReference/`. However, the rest of the processing of these URLs and the attempts to locate and process the
`BeanDefinitionReference` content fails, as Micronaut seems unprepared to handle a URL like this.

(For comparison, the URLs while running in dev mode, debugging the app in IntelliJ start with `jar:file:/path...`).

It is unknown whether this is to spec or not (whatever spec that might be... servlet?).
It is also unknown whether this is something configurable within WebLogic 12c, or if this is to be expected.

### Setting up Oracle WebLogic 12c

Skip this part if you already have WebLogic available. (It is assumed you already have docker installed.)

1. You need to have an Oracle account. If you don't,
   [this page](https://profile.oracle.com/myprofile/account/create-account.jspx) should let you create an account.
2. Login to the Oracle Container Registry.
   ```
   $ docker login container-registry.oracle.com
   Username: user@example.com
   Password: ********
   Login Succeeded
   ```
3. Create a local directory to be mapped to the container. Ex: `mkdir ~/apps/docker/oracle-weblogic-12c`
4. In that directory, create a file `domain.properties` with the following content:
   ```
   username=admin
   password=admin123
   ```
   You can use a different username and password you like.
   This will be the username/password you use to login to the administration console.
5. Install the WebLogic docker container.
   Replace `~/apps/docker/oracle-weblogic-12c` in this command with the directory created in step 3 above, if different. 
   ```
   $ docker run -d -it --name wlsnode -p 7001:7001 -p 9002:9002 -v ~/apps/docker/oracle-weblogic-12c:/u01/oracle/properties container-registry.oracle.com/middleware/weblogic:12.2.1.4
   ```
6. You should now be able to access and login to the administration console at: `https://localhost:9002/console/`
7. Login to the admin console using the username/password you created in step 4 above.
8. If you want to monitor logs and standard out, you can with: `$ docker logs wlsnode --follow`

### Build the Micronaut war

Run `./gradlew war` from this directory (i.e. containing this README file and the `build.gradle` file);
this will generate the war file `build/libs/weblogic-war-0.1.war`.

### Installing the Micronaut war

Use the WebLogic Administration console to install the war. Note that some steps may show a spinning cursor and take
several seconds; don't interrupt the process! It will refresh when it's done.

1. Left side middle, "Domain Structure": click "Deployments".
2. Left side top, "Change Center": click "Lock & Edit".
3. Main body, "Summary of Deployments", "Configuration" tab: click "Install" (located just above the table).
4. Main body, "Install Application Assistant": click the link "Upload your file(s)" (located above the Path field).
5. Main body: Click the Deployment Archive button "Choose File". 
6. Locate and select your `weblogic-war-0.1.war` file.
7. Main body: Click the "Next" button.
8. Main body: You should see a "weblogic-war-0.1.war" radio button selected, and a message near the top that reads:
   "The file weblogic-war-0.1.war has been uploaded successfully to ...some path...". Click the "Next" button.
9. Main body: The radio "Install this deployment as an application" should be selected. Click "Next" button.
10. Main body: There will be a number of options; you can leave them all at defaults. Click "Finish" button.
11. Main body, "Summary of Deployments". You should see the message "The deployment has been successfully installed."
    You should see a new entry in the "Deployments" table for "weblogic-war-0.1".
12. Left side top, "Change Center": click "Activate Changes".
    Note that configuration changes are not committed/active until you click "Activate Changes".
13. After some spinning, the screen should refresh with a message at the top:
    "All changes have been activated. No restarts are necessary."

At some future point, if you need to delete the deployment, the steps will be similar: go to deployments, lock & edit,
switch to the "Configuration" tab, select "weblogic-war-0.1" in the table, click the "Delete" button, follow prompts,
and click "Activate Changes" to commit the change.

### Running the Micronaut war

Continuing where you left off on the "Summary of Deployments" page...

1. Main body: Directly under "Summary of Deployments", select the "Control" tab.
2. Main body: Select the checkbox in the table next to "weblogic-war-0.1".
3. Main body: At top of table, click "Start" popup menu, then click "Servicing all requests".
4. At this point, if you are not currently watching logs, you may want to with: `docker logs wlsnode --follow`.
5. Main body, "Start Application Assistant": Click the "Yes" button to start the app.

The steps to stop an application are similar. However, at this point, since the Micronaut application never fully starts,
you won't need to stop the app until the current problems are fixed.
