# Kotlin snowflake

## Description

- A Kotlin library for generating Snowflake IDs.

## How to  use

### Example

```kotlin
val nodeId: Long = 1234
val snowflake = Snowflake(nodeId)
snowflake.nextId()
```

### Add dependencies

#### Gradle(Kotlin)

```kotlin
implementation("io.github.crispindeity:kotlin-snowflake:0.0.1")
```

#### Gradle(Groovy)
```groovy
implementation group: 'io.github.crispindeity', name: 'kotlin-snowflake', version: '0.0.1'
```

#### Maven
```xml
<dependency>
    <groupId>io.github.crispindeity</groupId>
    <artifactId>kotlin-snowflake</artifactId>
    <version>0.0.1</version>
</dependency>
```
