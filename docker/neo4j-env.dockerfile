FROM neo4j:4.4-community⁠
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# install necessary software for neo4j.
RUN apt-get update && apt-get install -y --no-install-recommends \
  7zip wget curl gcc jq make procps tini \
  graphviz nodejs-legacy npm devscripts debhelper rpm unzip git \
  debhelper devscripts dos2unix dpkg xmlstarlet \
  && rm -rf /var/lib/apt/lists/*

# install maven
ENV MAVEN_VERSION 3.9.9
RUN wget -q "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" \
  && tar xzf "apache-maven-$MAVEN_VERSION-bin.tar.gz" -C /usr/share \
  && mv "/usr/share/apache-maven-$MAVEN_VERSION" /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn \
  && rm "apache-maven-$MAVEN_VERSION-bin.tar.gz"
# COPY maven-settings.xml /usr/share/maven/conf/settings.xml
ENV MAVEN_HOME /usr/share/maven

CMD /bin/bash
