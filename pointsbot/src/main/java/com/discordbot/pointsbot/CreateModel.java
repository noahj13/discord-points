package com.discordbot.pointsbot;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class CreateModel {
    private static Instances trainingData;
    private static StringToWordVector vector;
    private static FilteredClassifier classifier;
    private static String[] classes = {"good","bad","neutral"};

    private static void loadDataset(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
            trainingData = arff.getData();
            trainingData.setClassIndex(1);
            System.out.println("===== Loaded dataset: " + fileName + " =====");
            reader.close();
        }
        catch (IOException e) {
            System.out.println("Problem found when reading: " + fileName);
        }
    }

    private static void evaluate() {
        try {
            vector = new StringToWordVector();
            classifier = new FilteredClassifier();
            classifier.setFilter(vector);
            classifier.setClassifier(new NaiveBayes());
            Evaluation eval = new Evaluation(trainingData);
            eval.crossValidateModel(classifier, trainingData, 4, new Random(1));

            System.out.println("===== Evaluating on filtered (training) dataset done =====");
            System.out.println(eval.toSummaryString());
            System.out.println(eval.toClassDetailsString());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Problem found when evaluating");
        }
    }

    private static void learn() {
        try {
            classifier.setFilter(vector);
            classifier.setClassifier(new NaiveBayes());
            classifier.buildClassifier(trainingData);
            System.out.println("===== Training on filtered (training) dataset done =====");
            System.out.println(classifier);

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Problem found when training");
        }
    }


    private static void saveModel(String fileName) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
            out.writeObject(classifier);
            out.close();
            System.out.println("===== Saved model: " + fileName + " =====");
        }
        catch (IOException e) {
            System.out.println("Problem found when writing: " + fileName);
        }
    }

    private static void testData(String path){
        try {
            Instances test = new ArffLoader.ArffReader(new BufferedReader(new FileReader(path))).getData();
            test.setClassIndex(1);
            test.stream().forEach(i->{
                try {
                    i.setClassValue(classifier.classifyInstance(i));
                    System.out.println(i.stringValue(0) + ": " +i.stringValue(1));
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static String classifyMessage(String message){
        ArrayList<Attribute> atts = new ArrayList<Attribute>(2);
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("good");
        classVal.add("bad");
        classVal.add("neutral");
        atts.add(new Attribute("message",(ArrayList<String>)null));
        atts.add(new Attribute("class",classVal));

        Instances dataRaw = new Instances("messages",atts,1);
        dataRaw.setClassIndex(1);
        double[] instanceValue = new double[dataRaw.numAttributes()];

        instanceValue[0] = dataRaw.attribute(0).addStringValue(message);
        dataRaw.add(new DenseInstance(1.0, instanceValue));
        try {
            dataRaw.firstInstance().setClassValue(classifier.classifyInstance(dataRaw.firstInstance()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataRaw.firstInstance().stringValue(1);


    }

    public static void main(String[] args){
        CreateModel model = new CreateModel();
        model.loadDataset("pointsbot/Resources/messages.ARFF");
        model.evaluate();
        model.learn();
        model.saveModel("pointsbot/Resources/messagesClassifier.weka");
        model.testData("pointsbot/Resources/test.ARFF");
        Scanner kb = new Scanner(System.in);
        while(true)
            System.out.println(classifyMessage(kb.nextLine()));
    }
}
