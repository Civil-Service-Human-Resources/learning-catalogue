FROM java:8

ENV SPRING_PROFILES_ACTIVE production

ENV ELASTICSEARCH_URI http://127.0.0.1:9200
ENV ELASTICSEARCH_USER elastic
ENV ELASTICSEARCH_PASSWORD changeme

EXPOSE 9000

ADD environ/wait-for-it.sh .
ADD build/libs/learning-catalogue.jar /data/app.jar

# Add AppInsights config and agent jar
ADD lib/AI-Agent.xml /opt/appinsights/AI-Agent.xml
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.0.3/applicationinsights-agent-3.0.3.jar /opt/appinsights/applicationinsights-agent-3.0.3.jar

CMD java -javaagent:/opt/appinsights/applicationinsights-agent-3.0.3.jar -jar /data/app.jar