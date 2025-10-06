# aims-spark

[![Build Status](https://travis-ci.com/ONSdigital/address-index-data.svg?token=wrHpQMWmwL6kpsdmycnz&branch=develop)](https://travis-ci.com/ONSdigital/address-index-data)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/83c0fb7ca2e64567b0998848ca781a36)](https://www.codacy.com/app/Valtech-ONS/address-index-data?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/address-index-data&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/ONSdigital/address-index-data/branch/develop/graph/badge.svg)](https://codecov.io/gh/ONSdigital/address-index-data)



### Purpose

This repository contains the Scala code for an Apache Spark job to create an Elasticsearch index from the AddressBase premium product.

For testing purposes there is a free [AddressBase Premium sample](https://www.ordnancesurvey.co.uk/products/addressbase-premium/sample-data?assetid=29998369-99e4-4b0b-9efd-c2ab39c098ff) available from Ordnance Survey.
You will be required to register with OS and abide by the Data Exploration Licence

### Input Data

The data used by ONS to build the Elasticsearch indices is produced as follows:

Every 6 weeks we receive a set of AddressBase ‘Change Only Update’ (COU) files (the latest ‘Epoch’), which are applied to our previous existing AddressBase master tables to produce ‘full’ tables for the latest Epoch.
We run a <a href="dbscripts/hierarchy_script.sql">script</a> against the updated AddressBase data to produce an additional Hierarchy table.

The spark job will run without a real Hierarchy table (the dummy one in the unit tests will suffice, though it will need to be sited with the rest of the input data) but the "relatives" information will be blank on every document. This won't affect the automated matching process, rather the structure of hierarchical addresses helps with clerical resolution.

Note that the SQL used by ONS to create the Hierarchy table (link above) runs inside our Reference Data Management Framework (RDMF) system and not all the input fields are present in the standard ABP data: The address_entry_id and valid from / to dates are from the RDMF.

The job also has some additional input for the Integrated Data Service (IDS). This is a lookup between UPRN and AddressEntryId, the link key used internally with IDS and RDMF.

Again the dummy file will suffice to allow the job to complete, with the AddressEntryId field left blank on all records.

The configuration file reference.conf contains various settings such as the locations of the input files, these can be edited or overridden by creating an application.conf file in the same directory (more about this in the Running section below).

Example application.conf (files in GCS buckets as used by ONS):
```
addressindex.elasticsearch.nodes="10.210.0.13"
addressindex.elasticsearch.port="9200"
//
//addressindex.elasticsearch.user="elastic"
//addressindex.elasticsearch.pass="elastic"

// comment out the next two lines for a local run or if using Serverless Dataproc
// addressindex.spark.master="yarn"
// addressindex.spark.sql.shuffle.partitions=50
addressindex.spark.sql.broadcastTimeout="600s"
addressindex.islands.used="false"

addressindex.files.csv.blpu="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_blpu_current_20250912.csv"
addressindex.files.csv.classification="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_classification_current_20250912.csv"
addressindex.files.csv.crossref="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_crossref_current_20250912.csv"
addressindex.files.csv.delivery-point="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_delivery_point_current_20250912.csv"
addressindex.files.csv.hierarchy="gs://aims-ons-abp-raw-full-e119-179270555351/ABP_E811a_v111017.csv"
addressindex.files.csv.lpi="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_lpi_current_20250912.csv"
addressindex.files.csv.organisation="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_organisation_current_20250912.csv"
addressindex.files.csv.street-descriptor="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_street_descriptor_current_20250912.csv"
addressindex.files.csv.street="gs://aims-ons-abp-raw-full-e119-179270555351/ai_aims_street_current_20250912.csv"
addressindex.files.csv.rdmf="gs://aims-ons-rdmf-e119-179270555351/*"
```
Note that the islands flag is set to true if the islands data is supplied via separate CSV files, false if the islands data has been amalgamated or if it is not present. This flag may be removed soon as we no longer get the islands data separately.

The full indices are around 70GB in size and the skinny ones 40GB.

### Output Data

There are four different indices that can be output:
Full historic, Full non-historic, Skinny historic and Skinny non-historic, controlled by two input parameters to the Spark job (--skinny and --hybridNoHist).

The skinny index was developed to speed up the typeahead function for the 2021 Census. As well as having fewer fields, it also excludes address types that are not residential, commercial or educational. Bulk matching is always done using the full data.

As for historical entries, these are currently limited to those that ship with the latest Epoch. Our RDMF system has the ability to act as a "time machine" back to Epoch 39, but this is not currently implemented in AIMS.

### Indices for Specific LA(s)

The job can also be run to create indices for specific Local Authorities.
This is achieved using a runtime parameter called custCodes which requires a comma-separated list of Local Authority custodian codes.
for example:

         --custCodes=1720,1725

is Fareham and Gosport - list of codes here https://github.com/ONSdigital/aims-api/blob/main/parsers/src/main/resources/codelists/custodianList

The resultant indices are thus much smaller depending on the number of custodians selected and should be usable on a laptop.
However it is doing a WHERE clause on the full input data so the job to create them is still best run on a server or cloud service of some kind, such as Serverless Dataproc on Google Cloud Platform.
It can be run on a laptop but will take a long time, and may require some config changes.
For example, to run locally with input data in GCS buckets, you need to add
```
conf.set("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "C:\\Users\\user\\AppData\\Roaming\\gcloud\\application_default_credentials.json") 
//  the file is populated via gcloud auth application-default login and the path will depend on the OS in use, example above is Windows 11
conf.set("fs.gs.impl", classOf[com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem].getName)
```
to the SparkProvider class, and add
```
  "com.google.cloud.bigdataoss" % "gcs-connector" % "hadoop3-2.2.26"
```
to the localDeps variable in the build.sbt file.

### Software and Versions

* Java 17
* SBT 1.9.9
* Scala 2.13.14
* Apache Spark 3.5.1
* Elasticsearch 8.14.3
* Elasticsearch-spark-30 8.14.2
* Versions compatible with Google Dataproc Serverless v2.2

### Development Setup (IntelliJ)

* File, New, Project From Version Control, GitHub
* Git Repository URL - select https://github.com/ONSdigital/aims-spark
* Parent Directory: any local drive, typically your IdeaProjects directory
* Directory Name: aims-spark
* Clone

### Running

To package the project in a runnable fat-jar:
In `build.sbt` set the variable `localTarget` to `true` (if set to `false` the output will be a thin jar intended to run on platforms with Spark preloaded such as Cloudera and Google Dataproc).
From the root of the project

```shell
sbt clean assembly
```

The resulting jar will be located in `batch/target/scala-2.12/ons-ai-batch-assembly-version.jar`

To run the jar:

```shell
java -Dconfig.file=application.conf -jar batch/target/scala-2.13/ons-ai-batch-assembly-version.jar
```
The target Elasticsearch index can be on a local ES deployment or an external server (configurable)
The `application.conf` file may contain:

```
addressindex.elasticsearch.nodes="just-the-hostname.com"
addressindex.elasticsearch.pass="password"
```

These will override the default configuration. The location and names of the input files can also be overridden.
Note that these input files are extracted from AddressBase and subject to some pre-processing. For Dataproc runs the input files are stored in gs:// buckets

Other settings from the reference.conf can be overridden, e.g. for a cluster mode execution set
```
addressindex.spark.master="yarn"
```
If set, this line should be commented out for running tests

The job can also be run from inside IntelliJ.
In this case you can run the Main class directly but need to comment out lines 48-59 and uncomment the lines that follow:
```
val indexName = generateIndexName(historical=true, skinny=true, nisra=true)
val url = s"http://$nodes:$port/$indexName"
postMapping(skinny=true)
saveHybridAddresses(historical=true, skinny=true)
```
where the first boolean is for a historic index, second for a skinny index. Note that you can also hard-code the index name, and you also need to ensure
that the variable `localTarget` is set to true in `build.sbt`

You may have to add the following to the VM Options box in the run configuration
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED

## Running on Google Dataproc

The job can be run on serverless Dataproc by creating a batch similar to the example one below:
```yaml
  {
  "sparkBatch": {
    "jarFileUris": [
      "gs://my-bucket/ons-ai-batch-assembly-1720-1725.jar"
    ],
    "args": [
      "--custCodes=1720,1725"
    ],
    "mainClass": "uk.gov.ons.addressindex.Main"
  },
  "labels": {
    "goog-dataproc-location": "europe-west2"
  },
  "runtimeConfig": {
    "version": "2.2",
    "properties": {
      "spark.dataproc.driver.compute.tier": "premium",
      "spark.dataproc.executor.compute.tier": "premium",
      "spark.dataproc.driver.disk.tier": "premium",
      "spark.dataproc.executor.disk.tier": "premium",
      "spark.executor.instances": "2",
      "spark.driver.cores": "8",
      "spark.driver.memory": "24400m",
      "spark.executor.cores": "8",
      "spark.executor.memory": "12200m",
      "spark.dynamicAllocation.executorAllocationRatio": "0.3",
      "spark.app.name": "projects/my-project/locations/europe-west2/batches/twolas",
      "spark.dataproc.scaling.version": "2",
      "spark.dataproc.driver.disk.size": "750g",
      "spark.dataproc.executor.disk.size": "750g"
    }
  },
  "name": "projects/my-project/locations/europe-west2/batches/twolas",
  "environmentConfig": {
    "executionConfig": {
      "serviceAccount": "dataproc-create@my-project.iam.gserviceaccount.com",
      "subnetworkUri": "my-subnetwork",
      "networkTags": [
        "my-network"
      ]
    }
  }
}
```

## Running Tests

Before you can run tests on this project if using Windows you must

* Install the 64-bit version of winutils.exe https://github.com/kontext-tech/winutils/tree/master/hadoop-3.3.1/bin
* save it along with hadoop.dll on your local system in a bin directory e.g. c:\hadoop\bin
* create environment variables ```HADOOP_HOME = c:\hadoop and hadoop.home.dir = c:\hadoop\bin```
* Update Path to add ```%HADOOP_HOME%\bin```
* Make temp directory writeable, on command line: ```winutils chmod 777 C:\tmp\hive``` (or other location of hive directory)
* Now in IntelliJ you can mark the test directory (right-click, Mark Directory as, Test Resources Root).

Then next time you right-click the green arrow "Run ScalaTests" should be shown.

You may have to add the following to the VM Options box in the run configuration
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED

You should also check your application.conf and reference.conf to make sure they are pointing to the local unit test files.
### Dependencies

Top level project dependencies may be found in the build.sbt file.
