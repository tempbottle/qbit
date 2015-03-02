package io.advantageous.qbit.example;

import io.advantageous.qbit.QBit;
import io.advantageous.qbit.queue.QueueBuilder;
import io.advantageous.qbit.server.ServiceServer;
import io.advantageous.qbit.service.ServiceBundle;
import io.advantageous.qbit.service.ServiceQueue;
import io.advantageous.qbit.service.dispatchers.ServiceWorkers;
import org.boon.core.Sys;

import static io.advantageous.qbit.service.ServiceBundleBuilder.serviceBundleBuilder;
import static io.advantageous.qbit.service.ServiceProxyUtils.flushServiceProxy;

import java.util.List;

import static io.advantageous.qbit.service.ServiceBuilder.serviceBuilder;
import static io.advantageous.qbit.service.dispatchers.ServiceWorkers.workers;
import static org.boon.Lists.list;

/**
 * Created by rhightower on 2/20/15.
 */
public class PrototypeMain {

    public static void main(String... args) {


        QBit.factory().systemEventManager();



        final ServiceWorkers userDataServiceWorkers =
                workers(); //Create a round robin service dispatcher

        for (int index =0; index < 10; index++) {
            ServiceQueue userDataService = serviceBuilder()
                    .setQueueBuilder(QueueBuilder.queueBuilder().setBatchSize(1))
                    .setResponseQueueBuilder(QueueBuilder.queueBuilder().setBatchSize(1))
                    .setServiceObject(new UserDataService())
                    .build();
            userDataService.startCallBackHandler();
            userDataServiceWorkers.addService(userDataService);
        }

        userDataServiceWorkers.start();


        final ServiceBundle bundle = serviceBundleBuilder()
                .setAddress("/root").build();



        bundle.addServiceConsumer("/workers", userDataServiceWorkers);
        bundle.start();
        //bundle.startReturnHandlerProcessor();


        final UserDataServiceClient userDataServiceClient =
                bundle.createLocalProxy(UserDataServiceClient.class, "/workers");


        RecommendationService recommendationServiceImpl =
                new RecommendationService(userDataServiceClient);


        ServiceQueue recommendationServiceQueue = serviceBuilder()
                .setServiceObject(recommendationServiceImpl)
                .build().start().startCallBackHandler();

        RecommendationServiceClient recommendationServiceClient =
                recommendationServiceQueue.createProxy(RecommendationServiceClient.class);

        flushServiceProxy(recommendationServiceClient);
        Sys.sleep(1000);

        List<String> userNames = list("Bob", "Joe", "Scott", "William");

        userNames.forEach( userName->
                recommendationServiceClient.recommend(recommendations -> {
                    System.out.println("Recommendations for: " + userName);
                    recommendations.forEach(recommendation->
                            System.out.println("\t" + recommendation));
                }, userName)
        );



        flushServiceProxy(recommendationServiceClient);
        Sys.sleep(1000);

    }
}