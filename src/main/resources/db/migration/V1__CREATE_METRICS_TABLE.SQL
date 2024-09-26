CREATE TABLE resource_event_metrics(
    id VARCHAR(200) NOT NULL,
    fdkId VARCHAR(200),
    timestamp BIGINT,
    type VARCHAR(20),
    orgPath VARCHAR(200),
    removed BOOLEAN,
    isRelatedToTransportportal BOOLEAN,
    PRIMARY KEY (id)
);
