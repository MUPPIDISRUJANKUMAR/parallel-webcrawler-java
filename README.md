# Parallel Web Crawler Java

An advanced Java web crawler built using parallel processing with the ForkJoin Framework.

## Features
- Parallel web crawling using ForkJoinPool
- Thread-safe URL and word count handling
- JSON configuration and result output
- Dynamic proxy-based performance profiler
- Functional programming with Java Stream API
- Lightweight HTTP request throttling

## Technologies Used
- Java 17
- Maven
- Jackson
- Guice
- JUnit 5
- jsoup

## Run the Project

```bash
mvn package
```

```bash
java -classpath target/udacity-webcrawler-1.0.jar com.udacity.webcrawler.main.WebCrawlerMain src/main/config/sample_config.json
```

## Test the Project

```bash
mvn test
```
