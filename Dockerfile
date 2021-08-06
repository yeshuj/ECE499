FROM ucbbar/chipyard-image
COPY ./ chipyard/generators/ECE499
COPY ./software chipyard/tests/
COPY ./docker-resources/ECE499Config.scala chipyard/generators/chipyard/src/main/scala/config/
COPY ./docker-resources/test.sh .
COPY ./docker-resources/build.sbt chipyard/build.sbt
# RUN cd chipyard/sims/verilator && make -j2 CONFIG=Tutorial499Config
RUN chmod u+x test.sh
