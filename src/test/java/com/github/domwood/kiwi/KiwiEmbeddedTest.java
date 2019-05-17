package com.github.domwood.kiwi;

import com.github.domwood.kiwi.data.error.ApiError;
import com.github.domwood.kiwi.data.input.*;
import com.github.domwood.kiwi.data.output.*;
import com.github.domwood.kiwi.testutils.TestKafkaServer;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.domwood.kiwi.testutils.HttpTestUtils.asJsonPayload;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@ExtendWith({TestKafkaServer.class, SpringExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class KiwiEmbeddedTest {

    private static final int defaultTimeoutMs = 5000;

    @LocalServerPort
    private Integer serverPort;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String testKey = "testKey";
    private final String testPayload = "testPayload";
    private final Map<String, String> testHeaders = ImmutableMap.of("TestHeaderKey", "TestHeaderValue");


    @DisplayName("Test querying a topic that doesn't exist returns 404")
    @Test
    public void handleTopicDoesntExist(){
        ResponseEntity<ApiError> requestError =
                testRestTemplate.getForEntity("http://localhost:"+serverPort+"/api/topicInfo/missing", ApiError.class);

        assertEquals(HttpStatus.NOT_FOUND, requestError.getStatusCode());
        assertEquals(UnknownTopicOrPartitionException.class.getName(), requestError.getBody().rootCause());
    }


    @DisplayName("Test can create a topic on a kafka broker")
    @Test
    public void topicCreationTest(){
        String createdTestTopic = "createATestTopic";

        createTopicAndAwait(createdTestTopic);

        ResponseEntity<TopicInfo> topicInfoRequest =
                testRestTemplate.getForEntity("http://localhost:"+serverPort+"/api/topicInfo/"+createdTestTopic, TopicInfo.class);

        assertEquals(createdTestTopic, topicInfoRequest.getBody().topic());
        assertEquals(7, topicInfoRequest.getBody().partitionCount());
        assertEquals("compact", topicInfoRequest.getBody().configuration().get("cleanup.policy"));
    }

    @DisplayName("Test can delete a topic on a kafka broker")
    @Test
    public void topicDeleteTest(){
        String createdTestTopic = "createATopicToDelete";

        createTopicAndAwait(createdTestTopic);

        testRestTemplate.delete("http://localhost:"+serverPort+"/api/deleteTopic/"+createdTestTopic);

        await().atMost(defaultTimeoutMs, MILLISECONDS).until(() -> {
            ResponseEntity<TopicList> topicList =
                    testRestTemplate.getForEntity("http://localhost:"+serverPort+"/api/listTopics", TopicList.class);

            return topicList.getStatusCode().is2xxSuccessful() &&
                    ! topicList.getBody().topics().contains(createdTestTopic);
        });

    }


    @DisplayName("Test created topic will then be returned in a topic list query")
    @Test
    public void topicListTest(){
        String listableTopic = "topicShouldAppearInList";

        createTopicAndAwait(listableTopic);

        ResponseEntity<TopicList> topicList = awaitAndReturn(
                () -> testRestTemplate.getForEntity("http://localhost:"+serverPort+"/api/listTopics", TopicList.class),
                (response) -> response.getStatusCode().is2xxSuccessful() && response.getBody().topics().contains(listableTopic));

        assertThat(listableTopic, isIn(topicList.getBody().topics()));
    }

    @DisplayName("Test correctly produce anc consume a message from kafka")
    @SuppressWarnings("unchecked")
    @Test
    public void readWriteTest(){
        String readWriteTestTopic = "readWriteTest";

        createTopicAndAwait(readWriteTestTopic);

        ProducerRequest produce = buildProducerRequest(readWriteTestTopic);

        ResponseEntity<ProducerResponse> producerResponse =
                testRestTemplate.postForEntity("http://localhost:"+serverPort+"/api/produce", asJsonPayload(produce), ProducerResponse.class);

        assertTrue(producerResponse.getStatusCode().is2xxSuccessful());
        assertEquals(readWriteTestTopic, producerResponse.getBody().topic());

        ConsumerRequest consumerRequest = buildConsumerRequest(readWriteTestTopic);

        ResponseEntity<ConsumerResponse> consumerResponse = awaitAndReturn(
                () -> testRestTemplate.postForEntity("http://localhost:"+serverPort+"/api/consume", asJsonPayload(consumerRequest), ConsumerResponse.class),
                (response) -> response.getStatusCode().is2xxSuccessful() && response.getBody().messages().size() > 0);

        assertEquals(HttpStatus.OK, consumerResponse.getStatusCode());
        assertNotNull(consumerResponse.getBody());
        assertEquals(1, consumerResponse.getBody().messages().size());

        ConsumedMessage<String, String> message = (ConsumedMessage<String, String>) consumerResponse.getBody().messages().get(0);

        assertEquals(testKey, message.key());
        assertEquals(testPayload, message.message());
        assertEquals(testHeaders, message.headers());
    }


    private CreateTopicRequest createTopicRequest(String name){
        return ImmutableCreateTopicRequest
                .builder()
                .replicationFactor(1)
                .partitions(7)
                .name(name)
                .putConfiguration("cleanup.policy", "compact")
                .build();
    }

    private ProducerRequest buildProducerRequest(String topic){
        return ImmutableProducerRequest.builder()
                .topic(topic)
                .key(testKey)
                .payload(testPayload)
                .headers(testHeaders)
                .build();
    }

    private ConsumerRequest buildConsumerRequest(String topic){
        return ImmutableConsumerRequest.builder()
                .topics(singletonList(topic))
                .limit(1)
                .limitAppliesFromStart(false)
                .build();
    }

    private <T> ResponseEntity<T> awaitAndReturn(Supplier<ResponseEntity<T>> supplier, Predicate<ResponseEntity<T>> asserter){
        List<ResponseEntity<T>> latestResponse = new ArrayList<>(1);
        await().atMost(defaultTimeoutMs, MILLISECONDS).until(() -> {
            ResponseEntity<T> response = supplier.get();
            latestResponse.add(response);
            return asserter.test(response);
        });

        return latestResponse.get(latestResponse.size() - 1);
    }

    private void createTopicAndAwait(String createdTestTopic){
        CreateTopicRequest topicCreateRequest = createTopicRequest(createdTestTopic);

        ResponseEntity<Void> topicCreationResponse =
                testRestTemplate.postForEntity("http://localhost:"+serverPort+"/api/createTopic", asJsonPayload(topicCreateRequest), Void.class);

        assertTrue(topicCreationResponse.getStatusCode().is2xxSuccessful());

        awaitAndReturn(
                () -> testRestTemplate.getForEntity("http://localhost:"+serverPort+"/api/listTopics", TopicList.class),
                (response) -> response.getStatusCode().is2xxSuccessful() &&
                                response.getBody().topics().contains(createdTestTopic));
    }
}