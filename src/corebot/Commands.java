package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import corebot.ContentHandler.Map;
import mindustry.*;
import mindustry.game.*;
import mindustry.type.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import static corebot.CoreBot.*;

public class Commands {
    private final String prefix = "!";
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private CommandHandler handler = new CommandHandler(prefix);
    private CommandHandler adminHandler = new CommandHandler(prefix);
    private String[] warningStrings = { "один раз", "двічі", "тричі", "занадто багато разiв" };
    private Pattern invitePattern = Pattern
            .compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");

    Commands() {
        handler.register("help", "Показує усі команди бота.", args -> {
            if (messages.lastMessage.getChannel().getName().equalsIgnoreCase("multiplayer")) {
                messages.err("Використовуйте цю команду в #команди.");
                messages.deleteMessages();
                return;
            }

            StringBuilder builder = new StringBuilder();
            for (Command command : handler.getCommandList()) {
                builder.append(prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if (command.params.length > 0) {
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append("*");
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append("\n");
            }

            messages.info("Команди", builder.toString());
        });

        handler.register("ping", "<ip>", "Перевіряє затримку до серверу.", args -> {
            if (!messages.lastMessage.getChannel().getName().equalsIgnoreCase("команди")) {
                messages.err("Використовуйте цю команду в #команди.");
                messages.deleteMessages();
                return;
            }

            net.pingServer(args[0], result -> {
                if (result.name != null) {
                    messages.info("Сервер в мережі", "Хост: @\nГравці: @\nМапа: @\nХвиля: @\nВерсія: @\nЗатримка: @ms",
                            result.name, result.players, result.mapname, result.wave, result.version, result.ping);
                } else {
                    messages.err("Сервер не в мережі", "Час вийшов.");
                }
            });
        });

        handler.register("info", "<тема>", "Показує інформацію про вибрану тему.", args -> {
            try {
                Info info = Info.valueOf(args[0]);
                messages.info(info.title, info.text);
            } catch (IllegalArgumentException e) {
                messages.err("Помилка", "Неправильна тема '@'.\nНаявні теми: *@*", args[0],
                        Arrays.toString(Info.values()));
                messages.deleteMessages();
            }
        });

        handler.register("postmod", "<github-url>", "Post a plugin via Github repository URL.", args -> {
            if (!args[0].startsWith("https") || !args[0].contains("github")) {
                messages.err("(RU) Эта ссылка не является ссылкой на GitHub.");
            } else {
                try {
                    Document doc = Jsoup.connect(args[0]).get();

                    EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor)
                            .setColor(messages.normalColor)
                            .setAuthor(messages.lastUser.getName(), messages.lastUser.getAvatarUrl(),
                                    messages.lastUser.getAvatarUrl())
                            .setTitle(doc.select("strong[itemprop=name]").text());

                    Elements elem = doc.select("span[itemprop=about]");
                    if (!elem.isEmpty()) {
                        builder.addField("(RU) О моде", elem.text(), false);
                    }

                    builder.addField("(RU) Ссылка", args[0], false);

                    builder.addField("(RU) Скачать", args[0] + (args[0].endsWith("/") ? "" : "/") + "releases", false);

                    messages.channel.getGuild().getTextChannelById(modChannelID).sendMessage(builder.build()).queue();

                    messages.text("(RU) *Мод отправлен.*");
                } catch (IOException e) {
                    e.printStackTrace();
                    messages.err("(RU) Не удалось отобразить данные из URL.");
                }
            }
        });

        handler.register("postmap", "Надсилає .msav файл в канал #мапи.", args -> {
            Message message = messages.lastMessage;

            if (message.getAttachments().size() != 1
                    || !message.getAttachments().get(0).getFileName().endsWith(".msav")) {
                messages.err("Ви повинні мати лише один .msav файл в одному повідомленні!");
                messages.deleteMessages();
                return;
            }

            Attachment a = message.getAttachments().get(0);

            try {
                Map map = contentHandler.readMap(net.download(a.getUrl()));
                new File("cache/").mkdir();
                File mapFile = new File("cache/" + a.getFileName());
                Fi imageFile = Fi.get("cache/image_" + a.getFileName().replace(".msav", ".png"));
                Streams.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile.file());

                EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor).setColor(messages.normalColor)
                        .setImage("attachment://" + imageFile.name())

                        .setAuthor(messages.lastUser.getName(), messages.lastUser.getAvatarUrl(),
                                messages.lastUser.getAvatarUrl())
                        .setTitle(map.name == null ? a.getFileName().replace(".msav", "") : map.name);

                if (map.description != null)
                    builder.setFooter(map.description);

                messages.channel.getGuild().getTextChannelById(mapsChannelID).sendFile(mapFile)
                        .addFile(imageFile.file()).embed(builder.build()).queue();

                messages.text("*Мапу успішно надіслано.*");
            } catch (Exception e) {
                e.printStackTrace();
                messages.err("Сталася помилка при парсинґу мапи.", Strings.neatError(e, true));
                messages.deleteMessages();
            }
        });

        handler.register("google", "<фраза...>", "Дай мені загуглити для тебе.", args -> {
            try {
                messages.text("http://lmgtfy.com/?q=@", URLEncoder.encode(args[0], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        handler.register("cleanmod", "(RU) Почистить архив с модом. Меняет json на hjson и форматирует код.",
                args -> {
                    Message message = messages.lastMessage;

                    if (message.getAttachments().size() != 1
                            || !message.getAttachments().get(0).getFileName().endsWith(".zip")) {
                        messages.err("Ви повинні мати лише один .zip файл в одному повідомленні!");
                        messages.deleteMessages();
                        return;
                    }

                    Attachment a = message.getAttachments().get(0);

                    if (a.getSize() > 1024 * 1024 * 6) {
                        messages.err("(RU) Файл по размеру не может превышать 6 MB.");
                        messages.deleteMessages();
                    }

                    try {
                        new File("cache/").mkdir();
                        File baseFile = new File("cache/" + a.getFileName());
                        Fi destFolder = new Fi("dest_mod" + a.getFileName());
                        Fi destFile = new Fi("cache/" + new Fi(baseFile).nameWithoutExtension() + "-cleaned.zip");

                        if (destFolder.exists())
                            destFolder.deleteDirectory();
                        if (destFile.exists())
                            destFile.delete();

                        Streams.copy(net.download(a.getUrl()), new FileOutputStream(baseFile));
                        ZipFi zip = new ZipFi(new Fi(baseFile.getPath()));
                        zip.walk(file -> {
                            Fi output = destFolder
                                    .child(file.extension().equals("json") ? file.pathWithoutExtension() + ".hjson"
                                            : file.path());
                            output.parent().mkdirs();

                            if (file.extension().equals("json") || file.extension().equals("hjson")) {
                                output.writeString(fixJval(Jval.read(file.readString())).toString(Jformat.hjson));
                            } else {
                                file.copyTo(output);
                            }
                        });

                        try (OutputStream fos = destFile.write(false, 2048);
                                ZipOutputStream zos = new ZipOutputStream(fos)) {
                            for (Fi add : destFolder.findAll(f -> true)) {
                                if (add.isDirectory())
                                    continue;
                                zos.putNextEntry(new ZipEntry(add.path().substring(destFolder.path().length())));
                                Streams.copy(add.read(), zos);
                                zos.closeEntry();
                            }

                        }

                        messages.channel.sendFile(destFile.file()).queue();

                        messages.text("(RU) *Мод успешно почищен.*");
                    } catch (Throwable e) {
                        e.printStackTrace();
                        messages.err("(RU) Ошибка при открытии мода.", Strings.neatError(e, false));
                        messages.deleteMessages();
                    }
                });

        adminHandler.register("userinfo", "<@user>", "Отримати інформацію про користувача.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!"))
                author = author.substring(1);
            try {
                long l = Long.parseLong(author);
                User user = messages.jda.getUserById(l);
                Member member = messages.guild.getMember(user);

                if (member == null) {
                    messages.err("(RU) Этот пользователь не существует. Как это *вообще* произошло?");
                } else {
                    messages.info("Info for " + member.getEffectiveName(),
                            "Нік: @\nІм'я користувача: @\nID: @\nСтатус: @\nРолі: @\nЧи адмін: @\nЧас приєднання: @",
                            member.getNickname(), user.getName(), member.getIdLong(), member.getOnlineStatus(),
                            member.getRoles().stream().map(Role::getName).collect(Collectors.toList()), isAdmin(user),
                            member.getTimeJoined());
                }
            } catch (Exception e) {
                e.printStackTrace();
                messages.err("Недійсний формат імені, або користувача немає на сервері.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("warnings", "<@користувач>", "Отримати кількість наявних попереджень користувача.",
                args -> {
                    String author = args[0].substring(2, args[0].length() - 1);
                    if (author.startsWith("!"))
                        author = author.substring(1);
                    try {
                        long l = Long.parseLong(author);
                        User user = messages.jda.getUserById(l);
                        var list = getWarnings(user);
                        messages.text("Користувач '@' має **@** @.\n@", user.getName(), list.size, list.size == 1 ? "попередження" : "попереджень",
                        list.map(s -> {
                                    String[] split = s.split(":::");
                                    long time = Long.parseLong(split[0]);
                                    String warner = split.length > 1 ? split[1] : null,
                                            reason = split.length > 2 ? split[2] : null;
                                    return "- `" + fmt.format(new Date(time)) + "`: Вiдалиться через "
                                            + (30 - Duration.ofMillis((System.currentTimeMillis() - time)).toDays())
                                            + " дн." + (warner == null ? "" : "\n  ↳ *Від:* " + warner)
                                            + (reason == null ? "" : "\n  ↳ *Причина:* " + reason);
                                }).toString("\n"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        messages.err("Недійсний формат імені.");
                        messages.deleteMessages();
                    }
                });

        adminHandler.register("delete", "<кількість>", "Видалити певну кількість повідомлень.", args -> {
            try {
                int number = Integer.parseInt(args[0]) + 1;
                MessageHistory hist = messages.channel.getHistoryBefore(messages.lastMessage, number).complete();
                messages.channel.deleteMessages(hist.getRetrievedHistory()).queue();
                Log.info("Видалено @ повідомлень.", number);
            } catch (NumberFormatException e) {
                messages.err("Недійсне число.");
            }
        });

        adminHandler.register("warn", "<@користувач> [причина...]", "Видати попередження користувачу.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!"))
                author = author.substring(1);
            try {
                long l = Long.parseLong(author);
                User user = messages.jda.getUserById(l);
                var list = getWarnings(user);
                list.add(System.currentTimeMillis() + ":::" + messages.lastUser.getName()
                        + (args.length > 1 ? ":::" + args[1] : ""));
                messages.text("**@**, вас було попереджено *@*.", user.getAsMention(),
                        warningStrings[Mathf.clamp(list.size - 1, 0, warningStrings.length - 1)]);
                prefs.putArray("warning-list-" + user.getIdLong(), list);
                if (list.size >= 3) {
                    messages.lastMessage.getGuild().getTextChannelById(moderationChannelID)
                            .sendMessage(
                                    "Користувача " + user.getAsMention() + " було попереджено тричі або більше разів!")
                            .queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                messages.err("Недійсний формат імені.");
                messages.deleteMessages();
            }
        });

        adminHandler.register("clearwarnings", "<@користувач>", "Очищує видані попередження.", args -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if (author.startsWith("!"))
                author = author.substring(1);
            try {
                long l = Long.parseLong(author);
                User user = messages.jda.getUserById(l);
                prefs.putArray("warning-list-" + user.getIdLong(), new Seq<>());
                messages.text("Попередження для користувача '@' очищено.", user.getName());
            } catch (Exception e) {
                e.printStackTrace();
                messages.err("Недійсний формат імені.");
                messages.deleteMessages();
            }
        });
    }

    private Seq<String> getWarnings(User user) {
        var list = prefs.getArray("warning-list-" + user.getIdLong());
        // remove invalid warnings
        list.removeAll(s -> {
            String[] split = s.split(":::");
            return Duration.ofMillis((System.currentTimeMillis() - Long.parseLong(split[0]))).toDays() >= 30;
        });

        return list;
    }

    private Jval fixJval(Jval val) {
        if (val.isArray()) {
            Seq<Jval> list = val.asArray().copy();
            for (Jval child : list) {
                if (child.isObject() && (child.has("item")) && child.has("amount")) {
                    val.asArray().remove(child);
                    val.asArray().add(Jval.valueOf(
                            child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                } else {
                    fixJval(child);
                }
            }
        } else if (val.isObject()) {
            Seq<String> keys = val.asObject().keys().toArray();

            for (String key : keys) {
                Jval child = val.get(key);
                if (child.isObject() && (child.has("item")) && child.has("amount")) {
                    val.remove(key);
                    val.add(key, Jval.valueOf(
                            child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                } else {
                    fixJval(child);
                }
            }
        }

        return val;
    }

    boolean isAdmin(User user) {
        var member = messages.guild.getMember(user);
        return member != null && member.getRoles().stream()
                .anyMatch(role -> role.getName().equals("admin") || role.getName().equals("moderator"));
    }

    boolean checkInvite(Message message) {
        if (invitePattern.matcher(message.getContentRaw()).find() && !isAdmin(message.getAuthor())
                && message.getChannel().getType() != ChannelType.PRIVATE) {
            Log.warn("User @ just sent a discord invite in @.", message.getAuthor().getName(),
                    message.getChannel().getName());
            message.delete().queue();
            message.getAuthor().openPrivateChannel().complete().sendMessage(
                    "Не надсилайте запрошувальні посилання на українськомовний Mindustry сервер. Читайте #правила.")
                    .queue();
            return true;
        }
        return false;
    }

    void handle(Message message) {
        if (message.getAuthor().isBot() || message.getChannel().getType() != ChannelType.TEXT)
            return;

        if (checkInvite(message)) {
            return;
        }

        if ((message.getChannel().getIdLong() == screenshotsChannelID
                || message.getChannel().getIdLong() == artChannelID) && message.getAttachments().isEmpty()) {
            message.delete().queue();
            try {
                message.getAuthor().openPrivateChannel().complete()
                        .sendMessage("Не надсилайте повідомлення без зображення в цьому каналі.").queue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        String text = message.getContentRaw();

        if (message.getContentRaw().startsWith(prefix)) {
            messages.channel = message.getTextChannel();
            messages.lastUser = message.getAuthor();
            messages.lastMessage = message;
        }

        // schematic preview
        if ((message.getContentRaw().startsWith(ContentHandler.schemHeader) && message.getAttachments().isEmpty())
                || (message.getAttachments().size() == 1 && message.getAttachments().get(0).getFileExtension() != null
                        && message.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension))) {
            try {
                Schematic schem = message.getAttachments().size() == 1
                        ? contentHandler.parseSchematicURL(message.getAttachments().get(0).getUrl())
                        : contentHandler.parseSchematic(message.getContentRaw());
                BufferedImage preview = contentHandler.previewSchematic(schem);

                new File("cache").mkdir();
                File previewFile = new File("cache/img_" + UUID.randomUUID().toString() + ".png");
                File schemFile = new File(schem.name() + "." + Vars.schematicExtension);
                Schematics.write(schem, new Fi(schemFile));
                ImageIO.write(preview, "png", previewFile);

                EmbedBuilder builder = new EmbedBuilder().setColor(messages.normalColor).setColor(messages.normalColor)
                        .setImage("attachment://" + previewFile.getName()).setAuthor(message.getAuthor().getName(),
                                message.getAuthor().getAvatarUrl(), message.getAuthor().getAvatarUrl())
                        .setTitle(schem.name());

                if (!schem.description().isEmpty())
                    builder.setFooter(schem.description());

                StringBuilder field = new StringBuilder();

                for (ItemStack stack : schem.requirements()) {
                    List<Emote> emotes = messages.guild.getEmotesByName(stack.item.name.replace("-", ""), true);
                    Emote result = emotes.isEmpty() ? messages.guild.getEmotesByName("ohno", true).get(0)
                            : emotes.get(0);

                    field.append(result.getAsMention()).append(stack.amount).append("  ");
                }
                builder.addField("(RU) Требования", field.toString(), false);

                message.getChannel().sendFile(schemFile).addFile(previewFile).embed(builder.build()).queue();
                message.delete().queue();
            } catch (Throwable e) {
                if (message.getTextChannel().getIdLong() == schematicsChannelID
                        || message.getTextChannel().getIdLong() == baseSchematicsChannelID) {
                    message.delete().queue();
                    try {
                        message.getAuthor().openPrivateChannel().complete()
                                .sendMessage("Недісні схеми: " + e.getClass().getSimpleName()
                                        + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")"))
                                .queue();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }

                Log.err("Failed to parse schematic, skipping.");
                Log.err(e);
            }
        } else if ((message.getTextChannel().getIdLong() == schematicsChannelID
                || message.getTextChannel().getIdLong() == schematicsChannelID) && !isAdmin(message.getAuthor())) {
            message.delete().queue();
            try {
                message.getAuthor().openPrivateChannel().complete().sendMessage(
                        "Надсилайте тільки дійсні схеми до каналу #схеми. Ви можете надіслати схему текстом з клавіатури або файлом.").queue();
                        
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (!text.trim().equals("!")) {
            if (isAdmin(message.getAuthor())) {
                boolean unknown = handleResponse(adminHandler.handleMessage(text), false);
                handleResponse(handler.handleMessage(text), !unknown);
            } else {
                handleResponse(handler.handleMessage(text), true);
            }
        }
    }

    boolean handleResponse(CommandResponse response, boolean logUnknown) {
        if (response.type == ResponseType.unknownCommand) {
            if (logUnknown) {
                messages.err("Невідома команда. Напишіть !help, щоб отримати список усіх команд.");
                messages.deleteMessages();
            }
            return false;
        } else if (response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments) {
            if (response.command.params.length == 0) {
                messages.err("Недійсні аргументи.", "Використання: @@", prefix, response.command.text);
                messages.deleteMessages();
            } else {
                messages.err("Недійсні аргументи.", "Використання: @@ *@*", prefix, response.command.text,
                        response.command.paramText);
                messages.deleteMessages();
            }
            return false;
        }
        return true;
    }

}
