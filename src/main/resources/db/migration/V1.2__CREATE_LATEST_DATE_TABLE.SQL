CREATE TABLE latest_for_date(
    fdkId VARCHAR(200) NOT NULL,
    calculatedForDate DATE NOT NULL,
    statId VARCHAR(200) NOT NULL REFERENCES statistics,
    PRIMARY KEY (fdkId, calculatedForDate)
);
