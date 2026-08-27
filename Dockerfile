#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at

#       https://www.apache.org/licenses/LICENSE-2.0

#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean test-compile 

# Stage 2: Immagine di runtime leggera
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia i file compilati dallo stage di build
COPY --from=builder /app/target/classes ./target/classes
COPY --from=builder /app/target/test-classes ./target/test-classes

EXPOSE 8080

# Avvia l'applicazione demo
ENTRYPOINT ["java", "-cp", "target/classes:target/test-classes", "org.apache.commons.collections4.demo.DemoApp"]