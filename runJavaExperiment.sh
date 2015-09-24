#!/bin/bash
#$ -S /bin/bash
# ------ send output to particular files, by job and task id numbers
#$ -o /dev/null
#$ -e /data/people/$USER/error/$JOB_ID.err

echo $JOB_ID $1 $2 $3 $4 $5 >> /data/people/$USER/error/$JOB_ID.err

/gpfs/main/research/java/linux/jdk1.8.0_25/bin/java -Dfile.encoding=UTF-8 -classpath /gpfs/main/home/$USER/git_probMods/cognitivehierarchy/bin:/gpfs/main/home/$USER/git_probMods/burlap/bin:/gpfs/main/home/$USER/git_probMods/burlap/lib/colt-1.2.0.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/commons-beanutils-1.6.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/commons-collections-2.1.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/commons-lang3-3.1.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/commons-logging-1.1.1.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/commons-math3-3.2.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/csparsej-1.1.1.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/ejml-0.25.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/hamcrest-core-1.3.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/jackson-annotations-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/jackson-core-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/jackson-databind-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/JavaRLGlueCodec.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/jcommon-1.0.21.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/jfreechart-1.0.17.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/joptimizer-3.2.0.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/joptimizer-3.3.0.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/junit-4.11.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/log4j-1.2.14.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/LPSOLVESolverPack.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/SCPSolver.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/servlet.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/snakeyaml-1.13.jar:/gpfs/main/home/$USER/git_probMods/burlap/lib/xml-apis-1.0.b2.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/bin:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jackson-annotations-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jackson-core-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jackson-databind-2.2.3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty-all-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/annotations/asm-5.0.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/annotations/asm-commons-5.0.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/annotations/javax.annotation-api-1.2.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jsp/org.eclipse.jetty.apache-jsp-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jsp/org.eclipse.jetty.orbit.org.eclipse.jdt.core-3.8.2.v20130121.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jsp/org.mortbay.jasper.apache-el-8.0.9.M3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jsp/org.mortbay.jasper.apache-jsp-8.0.9.M3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jstl/org.apache.taglibs.taglibs-standard-impl-1.2.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/apache-jstl/org.apache.taglibs.taglibs-standard-spec-1.2.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/fcgi/fcgi-client-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/fcgi/fcgi-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jaspi/javax.security.auth.message-1.0.0.v201108011116.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-alpn-client-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-alpn-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-annotations-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-cdi-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-client-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-continuation-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-deploy-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-http-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-io-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-jaas-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-jaspi-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-jmx-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-jndi-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-plus-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-proxy-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-quickstart-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-rewrite-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-schemas-3.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-security-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-servlet-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-servlets-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-util-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-webapp-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jetty-xml-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jndi/javax.mail.glassfish-1.4.1.v201005082020.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jndi/javax.transaction-api-1.2.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/javax.el-3.0.0.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/javax.servlet.jsp-2.3.2.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/javax.servlet.jsp-api-2.3.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/javax.servlet.jsp.jstl-1.2.2.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/jetty-jsp-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/jetty-jsp-jdt-2.3.3.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/org.eclipse.jdt.core-3.8.2.v20130121.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/jsp/org.eclipse.jetty.orbit.javax.servlet.jsp.jstl-1.2.0.v201105211821.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/monitor/jetty-monitor-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/servlet-api-3.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/setuid/jetty-setuid-java-1.0.1.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spdy/spdy-client-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spdy/spdy-core-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spdy/spdy-http-common-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spdy/spdy-http-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spdy/spdy-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/spring/jetty-spring-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/javax-websocket-client-impl-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/javax-websocket-server-impl-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/javax.websocket-api-1.0.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/websocket-api-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/websocket-client-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/websocket-common-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/websocket-server-9.2.10.v20150310.jar:/gpfs/main/home/$USER/git_probMods/MultiAgentGames/lib/jetty/websocket/websocket-servlet-9.2.10.v20150310.jar simulations.Experiment $1 $2 $3'_'$JOB_ID $4 $5

