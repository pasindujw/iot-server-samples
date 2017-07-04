
-- -----------------------------------------------------
--  Agent Database
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `androidglass_DEVICE` (
  `androidglass_DEVICE_ID` VARCHAR(45) NOT NULL ,
  `DEVICE_NAME` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`androidglass_DEVICE_ID`) );

CREATE TABLE IF NOT EXISTS `edge_DEVICE` (
  `androidglass_DEVICE_ID` VARCHAR(45) NOT NULL ,
  `SERIAL` VARCHAR(45) NOT NULL ,
  `DEVICE_NAME` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`SERIAL`) );



