/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.strom.kafka;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;

/**
 * This topology demonstrates Storm's stream groupings and multilang
 * capabilities.
 */
public class WordCountTopology {

	public static void main(String[] args) throws Exception {

		TopologyBuilder builder = new TopologyBuilder();

		// Configure Kafka
		String zks = "youzy.domain:2181,youzy2.domain:2181,youzy3.domain:2181";
		String topic = "my-topic-test";
		String zkRoot = "/storm";
		String id = "kafkaspout";

		BrokerHosts brokerHosts = new ZkHosts(zks);
		SpoutConfig spoutConf = new SpoutConfig(brokerHosts, topic, zkRoot, id);
		spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());

		spoutConf.forceFromStart = false;
		spoutConf.zkServers = Arrays.asList(new String[] { "youzy.domain", "youzy2.domain", "youzy3.domain" });
		spoutConf.zkPort = 2181;

		// @param1 节点id
		// @param2 实现类
		// @param3 线程并发数
		builder.setSpout("spout", new KafkaSpout(spoutConf), 4);

		builder.setBolt("split", new SplitSentence(), 4).shuffleGrouping("spout");

		builder.setBolt("count", new WordCount(), 4).shuffleGrouping("split");

		Config conf = new Config();
		conf.setDebug(true);
		conf.setNumWorkers(2);

		if (args != null && args.length > 0) {

			conf.put(Config.NIMBUS_HOST, "youzy2.domain");
			StormSubmitter.submitTopology(args[0], conf, builder.createTopology());

		} else {

			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology("word-count", conf, builder.createTopology());

			Thread.sleep(10000);
			cluster.killTopology("word-count");
			cluster.shutdown();

		}
	}
}
