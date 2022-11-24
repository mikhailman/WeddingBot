package com.verishko.WeddingBot.service;

import com.vdurmont.emoji.EmojiParser;
import com.verishko.WeddingBot.config.BotConfig;
import com.verishko.WeddingBot.model.User;
import com.verishko.WeddingBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    private final BotConfig config;

    private static final String HELP_TEXT = "This bot was created to celebrate Natasha and Pasha with their Wedding.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/register", "register in bot"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;

                case "/mydata":
                    sendDataMessage(update.getMessage());
                    break;

                case "/register":
                    register(chatId);
                    break;

                default:
                    sendMessage(chatId, "Sorry, " + update.getMessage().getChat().getFirstName() + ", command was not recognized :(");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals("YES_BUTTON")) {
                String text = "You pressed YES button";
                EditMessageText messageText = new EditMessageText();
                messageText.setChatId(chatId);
                messageText.setText(text);
                messageText.setMessageId((int) messageId);
                try {
                    execute(messageText);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callbackData.equals("NO_BUTTON")) {
                String text = "You pressed NO button";
                EditMessageText messageText = new EditMessageText();
                messageText.setChatId(chatId);
                messageText.setText(text);
                messageText.setMessageId((int) messageId);
                try {
                    execute(messageText);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            }
        }
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }

    }

    private void registerUser(Message msg) {

        if (userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user);
            log.info("Bio: " + chat.getBio());
            log.info("getDescription: " + chat.getDescription());
            log.info("getEmojiStatusCustomEmojiId: " + chat.getEmojiStatusCustomEmojiId());
            log.info("getInviteLink: " + chat.getInviteLink());
            log.info("getTitle: " + chat.getTitle());
            log.info("getType: " + chat.getType());
            log.info("getStickerSetName: " + chat.getStickerSetName());
            log.info("getCanSetStickerSet: " + chat.getCanSetStickerSet());
            log.info("getHasPrivateForwards: " + chat.getHasPrivateForwards());
            log.info("getHasProtectedContent: " + chat.getHasProtectedContent());
            log.info("getHasRestrictedVoiceAndVideoMessages: " + chat.getHasRestrictedVoiceAndVideoMessages());
            log.info("getId: " + chat.getId());
            log.info("getIsForum: " + chat.getIsForum());
            log.info("getActiveUsernames: " + chat.getActiveUsernames());
            log.info("getLocation: " + chat.getLocation());
            log.info("getJoinByRequest: " + chat.getJoinByRequest());
            log.info("getJoinToSendMessages: " + chat.getJoinToSendMessages());
            log.info("getPhoto: " + chat.getPhoto());
            log.info("getLinkedChatId: " + chat.getLinkedChatId());
            log.info("getPinnedMessage: " + chat.getPinnedMessage());
            log.info("getSlowModeDelay: " + chat.getSlowModeDelay());
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
//        String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendDataMessage(Message msg) {

        var chat = msg.getChat();

        List myData = new ArrayList<>();
        myData.add(chat.getId());
        if (chat.getUserName() != null) {
            myData.add(chat.getFirstName());
        }
        if (chat.getUserName() != null) {
            myData.add(chat.getUserName());
        }
        if (chat.getTitle() != null) {
            myData.add(chat.getTitle());
        }
        sendMessage(chat.getId(), myData.toString());
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("/start");
        row.add("/mydata");
        row.add("/help");
        row.add("/register");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
