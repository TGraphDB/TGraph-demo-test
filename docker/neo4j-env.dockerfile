FROM neo4j:4.4-community
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

USER root

# install necessary software for neo4j.
RUN apt-get update && apt-get install -y --no-install-recommends \
  wget curl unzip git miller \
  && rm -rf /var/lib/apt/lists/*

# install maven
ENV MAVEN_VERSION 3.9.9
RUN wget -nv "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" \
  && tar xzf "apache-maven-$MAVEN_VERSION-bin.tar.gz" -C /usr/share \
  && mv "/usr/share/apache-maven-$MAVEN_VERSION" /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn \
  && rm "apache-maven-$MAVEN_VERSION-bin.tar.gz"

# COPY maven-settings.xml /usr/share/maven/conf/settings.xml

ENV MAVEN_HOME /usr/share/maven

ENTRYPOINT []
CMD []
