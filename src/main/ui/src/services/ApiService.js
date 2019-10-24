import api from "./ApiConfig";
import {toast} from "react-toastify";

var activeCluster = null;

const doubleEncode = (param) => {
    return encodeURIComponent(encodeURIComponent(param))
};

const errorHandler = (error) => {
    console.error(error);
    toast.error(error);
};

const statusHandler = (response) => {
    if(response.ok){
        return response;
    }
    else{
        throw new Error(`${response.status} Error response from Server`);
    }
};

const wrapGetWithBrokers = (api, params) => {
    params = params ? activeCluster ? `&${params}` : `?${params}` : '';
    return activeCluster ? `${api}?bootStrapServers=${activeCluster}${params}` : `${api}${params}`;
};

const wrapPostWithBrokers = (request) => {
    return activeCluster ? Object.assign(request, {'bootStrapServers':activeCluster}) : request;
};

export const setActiveCluster = (brokers) => {
    activeCluster = brokers;
};

export const getActiveCluster = () => {
    return activeCluster;
}

export const getClusters = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(api.clusters)
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result))
        .catch(errorhandler);
};

export const getVersion = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(api.version)
        .then(statusHandler)
        .then(res => res.text())
        .then(result => cb(result))
        .catch(errorhandler);
};

export const getProfiles = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(api.profiles)
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result))
        .catch(errorhandler);
};

export const getTopics = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    return fetch(wrapGetWithBrokers(api.listTopics))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.topics))
        .catch(errorhandler);
};

export const getTopicInfo = (topic, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(`${api.topicInfo}/${doubleEncode(topic)}`))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result))
        .catch(errorhandler);
};

export const getBrokers = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(api.brokers))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.brokerInfo))
        .catch(errorhandler);
};

export const getLogs = (id, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(api.logs, `brokerId=${doubleEncode(id)}`))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.brokerLogInfo))
        .catch(errorhandler);
};

export const getAllConsumerGroupDetails = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(api.listConsumerGroupTopicDetails))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.topicDetails))
        .catch(errorhandler);
};

export const getConsumerGroups = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(api.listConsumerGroups))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.groups))
        .catch(errorhandler);
};


export const deleteConsumerGroup = (groupId, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(`${api.deleteConsumerGroup}/${doubleEncode(groupId)}`), {
        method: 'DELETE',
    })
    .then(statusHandler)
    .then(() => cb())
    .catch(errorhandler);
};

export const updateTopicConfig = (topic, config, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(api.updateTopicConfig, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(wrapPostWithBrokers({
            requestType: ".UpdateTopicConfig",
            topic: topic,
            config: config
        }))
    })
    .then(statusHandler)
    .then(() => cb())
    .catch(errorhandler);
};

export const getConsumerGroupsForTopic = (topic, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(`${api.consumerGroupsForTopic}/${doubleEncode(topic)}`))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.groups))
        .catch(errorhandler);
};


export const getConsumerGroupDetailsWithOffsets = (groupdId, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(`${api.listConsumerGroupDetailsWithOffsets}/${doubleEncode(groupdId)}`))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.offsets))
        .catch(errorhandler);
};

export const getCreateTopicConfig = (cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(api.createTopicConfig))
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result.configOptions))
        .catch(errorhandler);
};

export const createTopic = (topicData, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    topicData.requestType = ".CreateTopicRequest";

    fetch(api.createTopic, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(wrapPostWithBrokers(topicData))
    })
    .then(statusHandler)
    .then(() => cb())
    .catch(errorhandler);
};

export const deleteTopic = (topic, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    fetch(wrapGetWithBrokers(`${api.deleteTopic}/${doubleEncode(topic)}`), {
        method: 'DELETE',
    })
    .then(statusHandler)
    .then(() => cb())
    .catch(errorhandler);
};


export const produce = (topic, key, value, headers, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error);

    if(!topic){
        errorhandler(new Error("Topic must be defined to produce to kafka"));
    }
    else if(!key){
        errorhandler(new Error("Key must be defined to produce to kafka"));
    }
    else{
        fetch(api.produce, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(wrapPostWithBrokers({
                requestType: ".ProducerRequest",
                topic: topic,
                key: key,
                headers: headers||{},
                payload: value||null
            }))
        })
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result))
        .catch(errorhandler);
    }

};

export const consume = (topics, limit, fromStart, filters, cb, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error, "major");

    if(!topics){
        errorhandler(new Error("Topic must be defined to consume from kafka"));
    }
    else{
        fetch(api.consume, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(wrapPostWithBrokers({
                requestType: ".ConsumerRequest",
                topics: topics,
                limit: limit||1,
                limitAppliesFromStart: fromStart||false,
                filters: filters || []
            }))
        })
        .then(statusHandler)
        .then(res => res.json())
        .then(result => cb(result))
        .catch(errorhandler);
    }
};

export const consumeToFile = (topics, filters, fileType, columns, separator, eb) => {
    let errorhandler = (error) => (eb||errorHandler)(error, "major");

    if(!topics){
        errorhandler(new Error("Topic must be defined to consume from kafka"));
    }
    else{
        let data = JSON.stringify(wrapPostWithBrokers({
            requestType: ".ConsumerToFileRequest",
            topics: topics,
            limit: -1,
            limitAppliesFromStart: false,
            filters: filters || [],
            fileType: fileType,
            columns: columns,
            separator: separator
        }));
        let encoded = window.btoa(data);
        let request = `${api.consumeToFile}?request=${encodeURIComponent(encoded)}`;
        window.open(request, "_blank");
    }
};