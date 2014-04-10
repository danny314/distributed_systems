/**
 * Authors: 
 * Puneet Bansal
 */
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

    public class TextAnalyzer {

        public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
          private final static IntWritable one = new IntWritable(1);
          private Text word = new Text();

          private String contextword;
          private String queryword;
          
          public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
            String line = value.toString().toLowerCase();
            
            line = line.replaceAll("[^a-z ]", " ");
            
            Pattern contextPattern = Pattern.compile("\\b" + contextword + "\\b");
            Matcher contextMatcher = contextPattern.matcher(line);
            
            if (contextMatcher.find()) {
                Pattern queryPattern = Pattern.compile("\\b" + queryword + "\\b");
                Matcher queryMatcher = queryPattern.matcher(line);
            	while (queryMatcher.find()) {
                    output.collect(word, one);
            	}
            }
          }
          
  		public String getContextword() {
			return contextword;
		}

		public void setContextword(String contextword) {
			this.contextword = contextword;
		}

		public String getQueryword() {
			return queryword;
		}

		public void setQueryword(String queryword) {
			this.queryword = queryword;
		}

		@Override
		public void configure(JobConf job) {
			super.configure(job);
			setContextword(job.get("contextword").toLowerCase());
			setQueryword(job.get("queryword").toLowerCase());
		}          
        }

        public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
          public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
            int sum = 0;
            while (values.hasNext()) {
              sum += values.next().get();
            }
            output.collect(key, new IntWritable(sum));
          }
        }

        public static void main(String[] args) throws Exception {
          JobConf conf = new JobConf(TextAnalyzer.class);
          conf.setJobName("wordcount");

          conf.setOutputKeyClass(Text.class);
          conf.setOutputValueClass(IntWritable.class);

          conf.setMapperClass(Map.class);
          conf.setCombinerClass(Reduce.class);
          conf.setReducerClass(Reduce.class);
          
          conf.set("queryword", args[2]);
          conf.set("contextword", args[1]);

          conf.setInputFormat(TextInputFormat.class);
          conf.setOutputFormat(TextOutputFormat.class);

          FileInputFormat.setInputPaths(conf, new Path(args[3]));
          FileOutputFormat.setOutputPath(conf, new Path(args[4]));

          JobClient.runJob(conf);
        }


    }


