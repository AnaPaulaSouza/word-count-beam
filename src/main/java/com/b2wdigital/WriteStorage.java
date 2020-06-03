package com.b2wdigital;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

import java.awt.*;
import java.nio.charset.StandardCharsets;

public class WriteStorage {

    /**Aplicação Ana Paula**/
    public interface TesteOptions extends PipelineOptions {

        String getInputSubscription();
        void setInputSubscription(String value);

        /** Set this required option to specify where to write the output. */
        @Description("Path of the file to write to")
        @Validation.Required
        String getOutput();
        void setOutput(String value);
    }

    static void run(TesteOptions options){
        Pipeline p = Pipeline.create(options);

        PCollection<PubsubMessage> messagePCollection = p.apply("Read from Pubsub",
                PubsubIO.readMessages().fromSubscription(options.getInputSubscription()));

        PCollection<String> stringPCollection = messagePCollection.apply("Get message", ParDo.of(new DoFn<PubsubMessage, String>() {
            @ProcessElement
            public void processElement(ProcessContext context){
                PubsubMessage pubsubMessage = context.element();
                byte[] data = pubsubMessage.getPayload();

                String jsonString = new String(data, StandardCharsets.UTF_8);

                context.output(jsonString);
            }
        }));

        stringPCollection.apply("Window 5 minute", Window.into(FixedWindows.of(Duration.standardMinutes(1))))
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to(options.getOutput()));
        p.run().waitUntilFinish();
    }



    public static void main(String[] args){
        TesteOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(TesteOptions.class);
        run(options);
    }
}