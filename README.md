# Flowable External Worker Library for Java

[License:  
![license](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/flowable/flowable-engine/blob/main/LICENSE)

![Flowable Actions CI](https://github.com/flowable/flowable-external-client-java/actions/workflows/main.yml/badge.svg?branch=main)

An _External Worker Task_ in BPMN or CMMN is a task where the custom logic of that task is executed externally to Flowable, i.e. on another server.
When the process or case engine arrives at such a task, it will create an **external job**, which is exposed over the REST API.
Through this REST API, the job can be acquired and locked. Once locked, the custom logic is responsible for signalling over REST that the work is done and the process or case can continue.

This project makes implementing such custom logic in Java easy by not having the worry about the low-level details of the REST API and focus on the actual custom business logic. Integrations for other languages are available, too.

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


### Connection properties

The following properties show how you can connect to a Flowable instance running at `http://host.docker.internal:8090` using basic authentication:

```properties
flowable.external.worker.rest.base-url=http://host.docker.internal:8090
flowable.external.worker.rest.authentication.basic.username=admin
flowable.external.worker.rest.authentication.basic.password=test
```

The context path can also be changed, if necessary:

```properties
flowable.external.worker.rest.context-path=/external-job-api
```

## Authentication

There are two ways that the application can be authenticated with a Flowable installation:

* Using HTTP Basic authentication with a provided username and password, as shown in the example above.
* Using HTTP Bearer authentication with a provided access token. In that case the `.basic` properties are replaced by the `flowable.external.worker.rest.authentication.bearer.token=myTokenValue` property.

### Flowable Trial

Connecting an external worker to the Flowable Trial is simpler, since everything is pre-configured.
It's required to either use the user credentials or to pre-configure a personal access token, for example:


```properties
flowable.external.worker.rest.authentication.bearer.token=<personal-access-token>
```

### Advanced Properties

The following properties are for advanced usage. Only change these if you understand the consequences and the concept of the external worker properly.

* `flowable.external.worker.workerId` : gives the external worker a custom identifier, which is used to identify which external worker has locked an external worker job.
* `flowable.external.worker.concurrency` : The amount of threads available to poll and execute external worker jobs. By default `1`.
* `flowable.external.worker.lock-duration` : The amount of time an external job will be locked when acquired. If this time limit is reached, other external workers will be able to acquire the same job. By default 5 minutes.
* `flowable.external.worker.number-of-retries` : The number of times to retry acquiring new jobs on the Flowable server-side before giving up. By default, 5.
* `flowable.external.worker.number-of-tasks` : The amount of external worker tasks to acquire and lock in one go. The default is `1`.
* `flowable.external.worker.polling-interval` : The amount of time between polling for new external worker jobs. By default, 30 seconds.


## Plain Java

The following dependency needs to be added to start in a plain Java project without Spring Boot:

```xml
<dependency>
    <groupId>org.flowable.client</groupId>
    <artifactId>flowable-external-worker-client-java</artifactId>
    <version>${flowable-worker-client.version}</version>
</dependency>
```

The following is an example Flowable Worker that is retrieving jobs from the `myTopic` topic

```java
ExternalWorkerClient externalWorkerClient = RestExternalWorkerClient.create(
        "my-worker",
        JavaHttpClientRestInvoker.withBasicAuth("http://localhost:8090/external-job-api", "admin", "test"),
        new ObjectMapper()
);
List<AcquiredExternalWorkerJob> jobs = externalWorkerClient.createJobAcquireBuilder()
        .topic("myTopic")
        .lockDuration(Duration.ofMinutes(1L))
        .acquireAndLock();

for (AcquiredExternalWorkerJob job : jobs) {
    System.out.println("Executed job: " + job.getId());
    externalWorkerClient.createCompletionBuilder(job)
            .complete();
}
```

### Authentication

There are two ways that the application can be authenticated with a Flowable installation:

* Using HTTP Basic authentication with a provided username and password, as shown in the example above.
* Using HTTP Bearer authentication with a provided access token. In this case the `RestInvoker` can be created like this: `JavaHttpClientRestInvoker.withAccessToken("http://localhost:8090/external-job-api", "<your-access-token>")`
