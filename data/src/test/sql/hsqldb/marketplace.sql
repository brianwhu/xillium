SET DATABASE UNIQUE NAME HSQLDB35C6FA5FDD
SET DATABASE GC 0
SET DATABASE DEFAULT RESULT MEMORY ROWS 0
SET DATABASE EVENT LOG LEVEL 0
SET DATABASE SQL NAMES FALSE
SET DATABASE SQL REFERENCES FALSE
SET DATABASE SQL SIZE FALSE
SET DATABASE SQL TYPES FALSE
SET DATABASE SQL TDC DELETE TRUE
SET DATABASE SQL TDC UPDATE TRUE
SET DATABASE SQL TRANSLATE TTI TYPES TRUE
SET DATABASE SQL CONCAT NULLS TRUE
SET DATABASE SQL NULLS FIRST TRUE
SET DATABASE SQL UNIQUE NULLS TRUE
SET DATABASE SQL CONVERT TRUNCATE TRUE
SET DATABASE SQL AVG SCALE 0
SET DATABASE SQL DOUBLE NAN TRUE
SET DATABASE SQL LONGVAR IS LOB FALSE
SET DATABASE TRANSACTION CONTROL LOCKS
SET DATABASE DEFAULT ISOLATION LEVEL READ COMMITTED
SET DATABASE TRANSACTION ROLLBACK ON CONFLICT TRUE
SET DATABASE TEXT TABLE DEFAULTS ''
SET FILES WRITE DELAY 10
SET FILES BACKUP INCREMENT TRUE
SET FILES CACHE SIZE 10000
SET FILES CACHE ROWS 50000
SET FILES SCALE 1
SET FILES LOB SCALE 32
SET FILES DEFRAG 0
SET FILES NIO TRUE
SET FILES NIO SIZE 256
SET FILES LOG TRUE
SET FILES LOG SIZE 200
CREATE USER SA PASSWORD DIGEST 'd41d8cd98f00b204e9800998ecf8427e'
CREATE SCHEMA SA AUTHORIZATION DBA
SET SCHEMA SA
CREATE MEMORY TABLE SA.MEMBERSHIP(EMAIL VARCHAR(64) NOT NULL PRIMARY KEY,FIRST_NAME VARCHAR(32) NOT NULL,LAST_NAME VARCHAR(32) NOT NULL)
CREATE MEMORY TABLE SA.MEMBERPREF(EMAIL VARCHAR(64) NOT NULL PRIMARY KEY,LEVEL SMALLINT DEFAULT 0,TELEPHONE VARCHAR(12),FOREIGN KEY(EMAIL) REFERENCES SA.MEMBERSHIP(EMAIL))
CREATE MEMORY TABLE SA.PURCHASE(PURCHASE_ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 100001) NOT NULL PRIMARY KEY,EMAIL VARCHAR(64) NOT NULL,PURCHASE_DATE VARCHAR(12),FOREIGN KEY(EMAIL) REFERENCES SA.MEMBERSHIP(EMAIL))
ALTER TABLE SA.PURCHASE ALTER COLUMN PURCHASE_ID RESTART WITH 100001
CREATE MEMORY TABLE SA.MERCHANDISE(PRODUCT_ID INTEGER NOT NULL PRIMARY KEY,PRODUCT_NAME VARCHAR(256),PRODUCT_DESC VARCHAR(1024))
CREATE MEMORY TABLE SA.PURCHASE_ITEM(PURCHASE_ID INTEGER NOT NULL,PRODUCT_ID INTEGER NOT NULL,QUANTITY INTEGER NOT NULL,PRIMARY KEY(PURCHASE_ID,PRODUCT_ID),FOREIGN KEY(PURCHASE_ID) REFERENCES SA.PURCHASE(PURCHASE_ID),FOREIGN KEY(PRODUCT_ID) REFERENCES SA.MERCHANDISE(PRODUCT_ID))
ALTER SEQUENCE SYSTEM_LOBS.LOB_ID RESTART WITH 1
SET DATABASE DEFAULT INITIAL SCHEMA SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.YES_OR_NO TO PUBLIC
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.TIME_STAMP TO PUBLIC
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.CARDINAL_NUMBER TO PUBLIC
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.CHARACTER_DATA TO PUBLIC
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.SQL_IDENTIFIER TO PUBLIC
GRANT DBA TO SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.YES_OR_NO TO SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.TIME_STAMP TO SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.CARDINAL_NUMBER TO SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.CHARACTER_DATA TO SA
GRANT USAGE ON DOMAIN INFORMATION_SCHEMA.SQL_IDENTIFIER TO SA