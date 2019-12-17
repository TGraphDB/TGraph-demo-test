FROM paulosalgado/oracle-java8-ubuntu-16:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

# install necessary software for neo4j. # emacs23-nox
RUN apt-get update && apt-get install -y --no-install-recommends \
  subversion wget curl \
  graphviz nodejs-legacy npm make devscripts debhelper rpm unzip git \
  debhelper devscripts dos2unix dpkg make xmlstarlet \
  && rm -rf /var/lib/apt/lists/*

# install maven
ENV MAVEN_VERSION 3.3.9
RUN wget -q "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" \
  && tar xzf "apache-maven-$MAVEN_VERSION-bin.tar.gz" -C /usr/share \
  && mv "/usr/share/apache-maven-$MAVEN_VERSION" /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
ENV MAVEN_HOME /usr/share/maven
COPY maven-settings.xml /usr/share/maven/conf/settings.xml
# ENV MAVEN_OPTS '-Xmx512m'

RUN mkdir -p /maven
VOLUME /maven

ENTRYPOINT /bin/bash