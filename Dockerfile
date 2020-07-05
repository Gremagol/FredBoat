FROM gradle:4.10.2-jdk11-slim AS build

ENV ENV docker
ENV name=fredboat

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# Run container

FROM openjdk:11-jre-slim AS runtime

WORKDIR /opt/${name}/

RUN adduser --disabled-password --gecos '' ${name}; \
    chown ${name}:${name} -R /opt/${name}; \
    chmod u+w /opt/${name}; \
    chmod 0755 -R /opt/${name}

USER ${name}

COPY --from=build /home/gradle/src/FredBoat/FredBoat.jar .

EXPOSE 1356

ENTRYPOINT ["java", "-jar", "FredBoat.jar"]
