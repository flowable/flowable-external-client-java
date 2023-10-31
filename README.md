# Flowable External Worker Library for Java

To use Flowable together with a Java application external workers can be used.
This project allows to connect a Java library to Flowable.
To connect to a Flowable installation it is required [to authenticate](#authentication).
With the client it is possible to specify a callback method which is called for every job executed.

## Authentication

There are 2 ways that the application can be authenticated with a Flowable installation.

* Using HTTP Basic authentication with a provided username and password
* Using HTTP Bearer authentication with a provided access token

## Spring Boot Sample

The following dependency needs to be added to start using Flowable workers.

```xml
<dependency>
    <groupId>org.flowable.client.spring</groupId>
    <artifactId>flowable-external-worker-spring-boot-starter</artifactId>
    <version>${flowable-worker-client.version}</version>
</dependency>
```

The following is an example Flowable Worker that is retrieving jobs from the `myTopic` topic

```java
import org.springframework.stereotype.Component;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;

@Component
public class ExampleWorker {

    @FlowableWorker(topic = "myTopic")
    public void processJob(AcquiredExternalWorkerJob job) {
        System.out.println("Executed job: " + job.getId());
    }

}
```

Using the example above the job will be completed if the method completes successfully, and it will fail if the method throws an exception.

In order to pass additional data for a failure or a completion the following example can be used:

```java
import org.springframework.stereotype.Component;

import org.flowable.external.client.AcquiredExternalWorkerJob;
import org.flowable.external.worker.annotation.FlowableWorker;
import org.flowable.external.worker.WorkerResult;
import org.flowable.external.worker.WorkerResultBuilder;

@Component
public class ExampleWorker {

    @FlowableWorker(topic = "myTopic")
    public WorkerResult processJob(AcquiredExternalWorkerJob job, WorkerResultBuilder resultBuilder) {
        System.out.println("Executed job: " + job.getId());
        Object errorMessage = job.getVariables().get("errorMessage");
        if (errorMessage != null) {
            return resultBuilder.failure()
                    .message(errorMessage.toString())
                    .details("Error message details");
        }
        
        return resultBuilder.success()
                .variable("message", "Job has been executed");
    }
}
```


### Local

The following properties are example how you can connect to a Flowable instance running at `http://host.docker.internal:8090`

```properties
flowable.external.worker.rest.base-url=http://host.docker.internal:8090
flowable.external.worker.rest.authentication.basic.username=admin
flowable.external.worker.rest.authentication.basic.password=test
```

### Cloud

The usage with Flowable Cloud is simpler, since everything is pre-configured.
However, it's required to either use the user credentials or to pre-configure a personal access token.


```properties
flowable.external.worker.rest.authentication.bearer.token=<personal-access-token>
```
