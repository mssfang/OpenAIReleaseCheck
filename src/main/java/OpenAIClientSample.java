import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatMessageDelta;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.ai.openai.models.CompletionsUsage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Configuration;
import com.azure.core.util.IterableStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenAIClientSample {
    public static void main(String[] args) throws InterruptedException {
        String azureOpenaiKey = Configuration.getGlobalConfiguration().get("AZURE_OPENAI_KEY");
        String endpoint = Configuration.getGlobalConfiguration().get("AZURE_OPENAI_ENDPOINT");
        String deploymentOrModelId = "text-davinci-003";
        String chatCompletionID = "gpt-35-turbo";
        OpenAIClient client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(azureOpenaiKey))
                .buildClient();

        OpenAIAsyncClient asyncClient = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(azureOpenaiKey))
                .buildAsyncClient();

        // 1. streaming
        // text completions
        System.out.println("Beginning of streaming text completions SYNC API");
        streamCompletions(client, deploymentOrModelId);
        System.out.println("End of streaming text completions SYNC API");

        System.out.println("Beginning of streaming text completions ASYNC API");
        streamCompletionAsync(asyncClient, deploymentOrModelId);
        System.out.println("End of streaming text completions ASYNC API");

        // Chat completions
        System.out.println("Beginning of streaming chat completions SYNC API");
        streamChatCompletion(client, chatCompletionID);
        System.out.println("End of streaming chat completions SYNC API");

        System.out.println("Beginning of streaming chat completions ASYNC API");
        streamChatCompletionAsync(asyncClient, chatCompletionID);
        System.out.println("End of streaming chat completions ASYNC API");

        // 2. Non-streaming
        // text completions
        System.out.println("Beginning of non-streaming text completions SYNC API");
        nonStreamCompletion(client, deploymentOrModelId);
        System.out.println("End of non-streaming text completions SYNC API");

        System.out.println("Beginning of non-streaming text completions ASYNC API");
        nonStreamCompletionAsync(asyncClient, deploymentOrModelId);
        System.out.println("End of non-streaming text completions ASYNC API");

        // Chat completions
        System.out.println("Beginning of non-streaming chat completions SYNC API");
        nonStreamChatCompletion(client, chatCompletionID);
        System.out.println("End of non-streaming chat completions SYNC API");

        System.out.println("Beginning of non-streaming chat completions ASYNC API");
        nonStreamChatCompletionAsync(asyncClient, chatCompletionID);
        System.out.println("End of non-streaming chat completions ASYNC API");
    }

    public static void streamChatCompletion(OpenAIClient client, String deploymentOrModelId) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("Can you help me?"));
        chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("What's the best way to train a parrot?"));

        IterableStream<ChatCompletions> chatCompletionsStream = client.getChatCompletionsStream(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));

        chatCompletionsStream.forEach(chatCompletions -> {
            System.out.printf("Model ID=%s is created at %d.%n", chatCompletions.getId(), chatCompletions.getCreated());
            for (ChatChoice choice : chatCompletions.getChoices()) {
                ChatMessageDelta message = choice.getDelta();
                System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
                System.out.println("Message:");
                System.out.println(message.getContent());
            }

            CompletionsUsage usage = chatCompletions.getUsage();
            if (usage != null) {
                System.out.printf("Usage: number of prompt token is %d, "
                                + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                        usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
            }
        });
    }

    public static void streamChatCompletionAsync(OpenAIAsyncClient client, String deploymentOrModelId) throws InterruptedException {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("Can you help me?"));
        chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("What's the best way to train a parrot?"));

        client.getChatCompletionsStream(deploymentOrModelId, new ChatCompletionsOptions(chatMessages))
                .subscribe(chatCompletions -> {
                            System.out.printf("Model ID=%s is created at %d.%n", chatCompletions.getId(), chatCompletions.getCreated());
                            for (ChatChoice choice : chatCompletions.getChoices()) {
                                ChatMessageDelta message = choice.getDelta();

                                System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
                                System.out.println("Message:");
                                System.out.println(message.getContent());
                            }

                            CompletionsUsage usage = chatCompletions.getUsage();
                            if (usage != null) {
                                System.out.printf("Usage: number of prompt token is %d, "
                                                + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                                        usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                            }
                        },
                        error -> System.err.println("There was an error getting chat completions." + error),
                        () -> System.out.println("Completed called getChatCompletions."));


        // The .subscribe() creation and assignment is not a blocking call. For the purpose of this example, we sleep
        // the thread so the program does not end before the send operation is complete. Using .block() instead of
        // .subscribe() will turn this into a synchronous call.
        TimeUnit.SECONDS.sleep(10);
    }

    public static void nonStreamChatCompletion(OpenAIClient client, String deploymentOrModelId) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("Can you help me?"));
        chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("What's the best way to train a parrot?"));

        ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));

        System.out.printf("Model ID=%s is created at %d.%n", chatCompletions.getId(), chatCompletions.getCreated());
        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatMessage message = choice.getMessage();
            System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
            System.out.println("Message:");
            System.out.println(message.getContent());
        }

        System.out.println();
        CompletionsUsage usage = chatCompletions.getUsage();
        System.out.printf("Usage: number of prompt token is %d, "
                        + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

    }

    public static void nonStreamChatCompletionAsync(OpenAIAsyncClient client, String deploymentOrModelId) throws InterruptedException {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("You are a helpful assistant. You will talk like a pirate."));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("Can you help me?"));
        chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent("Of course, me hearty! What can I do for ye?"));
        chatMessages.add(new ChatMessage(ChatRole.USER).setContent("What's the best way to train a parrot?"));

        client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages)).subscribe(
                chatCompletions -> {
                    System.out.printf("Model ID=%s is created at %d.%n", chatCompletions.getId(), chatCompletions.getCreated());
                    for (ChatChoice choice : chatCompletions.getChoices()) {
                        ChatMessage message = choice.getMessage();
                        System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
                        System.out.println("Message:");
                        System.out.println(message.getContent());
                    }

                    System.out.println();
                    CompletionsUsage usage = chatCompletions.getUsage();
                    System.out.printf("Usage: number of prompt token is %d, "
                                    + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                            usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                },
                error -> System.err.println("There was an error getting chat completions." + error),
                () -> System.out.println("Completed called getChatCompletions."));


        // The .subscribe() creation and assignment is not a blocking call. For the purpose of this example, we sleep
        // the thread so the program does not end before the send operation is complete. Using .block() instead of
        // .subscribe() will turn this into a synchronous call.
        TimeUnit.SECONDS.sleep(10);
    }

    public static void nonStreamCompletionAsync(OpenAIAsyncClient client, String deploymentOrModelId)
            throws InterruptedException {
        List<String> prompt = new ArrayList<>();
        prompt.add("Why did the eagles not carry Frodo Baggins to Mordor?");

        client.getCompletions(deploymentOrModelId, new CompletionsOptions(prompt)).subscribe(
                completions -> {
                    System.out.printf("Model ID=%s is created at %d.%n", completions.getId(), completions.getCreated());
                    for (Choice choice : completions.getChoices()) {
                        System.out.printf("Index: %d, Text: %s.%n", choice.getIndex(), choice.getText());
                    }

                    CompletionsUsage usage = completions.getUsage();
                    System.out.printf("Usage: number of prompt token is %d, number of completion token is %d, "
                                    + "and number of total tokens in request and response is %d.%n",
                            usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                },
                error -> System.err.println("There was an error getting completions." + error),
                () -> System.out.println("Completed called getCompletions."));


        // The .subscribe() creation and assignment is not a blocking call. For the purpose of this example, we sleep
        // the thread so the program does not end before the send operation is complete. Using .block() instead of
        // .subscribe() will turn this into a synchronous call.
        TimeUnit.SECONDS.sleep(10);
    }

    public static void streamCompletions(OpenAIClient client, String deploymentOrModelId) {
        List<String> prompt = new ArrayList<>();
        prompt.add("Why did the eagles not carry Frodo Baggins to Mordor?");
        IterableStream<Completions> completionsStream = client.getCompletionsStream(deploymentOrModelId,
                new CompletionsOptions(prompt).setMaxTokens(1000).setStream(true));

        completionsStream.forEach(completions -> {
            System.out.printf("Model ID=%s is created at %d.%n", completions.getId(), completions.getCreated());
            for (Choice choice : completions.getChoices()) {
                System.out.printf("Index: %d, Text: %s.%n", choice.getIndex(), choice.getText());
            }

            CompletionsUsage usage = completions.getUsage();
            if (usage != null) {
                System.out.printf("Usage: number of prompt token is %d, "
                                + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                        usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
            }
        });
    }

    public static void nonStreamCompletion(OpenAIClient client, String deploymentOrModelId) {
        List<String> prompt = new ArrayList<>();
        prompt.add("Why did the eagles not carry Frodo Baggins to Mordor?");

        Completions completions = client.getCompletions(deploymentOrModelId, new CompletionsOptions(prompt));

        System.out.printf("Model ID=%s is created at %d.%n", completions.getId(), completions.getCreated());
        for (Choice choice : completions.getChoices()) {
            System.out.printf("Index: %d, Text: %s.%n", choice.getIndex(), choice.getText());
        }

        CompletionsUsage usage = completions.getUsage();
        System.out.printf("Usage: number of prompt token is %d, "
                        + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    public static void streamCompletionAsync(OpenAIAsyncClient client, String deploymentOrModelId) throws InterruptedException {


        List<String> prompt = new ArrayList<>();
        prompt.add("Why did the eagles not carry Frodo Baggins to Mordor?");

        client.getCompletionsStream(deploymentOrModelId,
                        new CompletionsOptions(prompt).setMaxTokens(1000).setStream(true))
                .subscribe(completions -> {
                    System.out.printf("Model ID=%s is created at %d.%n", completions.getId(), completions.getCreated());
                    for (Choice choice : completions.getChoices()) {
                        System.out.printf("Index: %d, Text: %s.%n", choice.getIndex(), choice.getText());
                    }

                    CompletionsUsage usage = completions.getUsage();
                    if (usage != null) {
                        System.out.printf("Usage: number of prompt token is %d, "
                                        + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
                                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                    }
                });

        // The .subscribe() creation and assignment is not a blocking call. For the purpose of this example, we sleep
        // the thread so the program does not end before the send operation is complete. Using .block() instead of
        // .subscribe() will turn this into a synchronous call.
        TimeUnit.SECONDS.sleep(10);
    }
}
