FROM frankescobar/allure-docker-service:latest 
USER root
RUN apt update && apt install unzip
