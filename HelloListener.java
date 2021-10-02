package codes.bronze.tutorial.listeners;

import java.io.*;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import codes.bronze.tutorial.ldcbot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class HelloListener<SlashCommandEvent> extends ListenerAdapter {
    //SSL驗證
    static {

        final TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null; // Not relevant.
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCertificates, new SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    Map<String, String> LOLNameMapObject = new HashMap<>();
    //偵測文字內容
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String contentRaw = event.getMessage().getContentRaw();
        String[] messageSent = contentRaw.split("\\s+");

        try {

            if (messageSent[0].equalsIgnoreCase("查LOL")) {
                String AccountIdTrue = this.getAccountId(event, messageSent[1]);
                if (AccountIdTrue.contains("404")) {
                    event.getChannel().sendMessage("無法查詢該召喚師。請確認名稱是否正確").queue();
                    return;
                }
                event.getChannel().sendMessage("正在獲取玩家資訊，約等待15秒。API由twlolstats.com提供").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                String SummonerDetail = this.getSummonerDetail(event, AccountIdTrue);
                String[] playerProfile = this.getPlayerProfile(event, SummonerDetail);
                String time = this.GetAddTime(playerProfile[0]);
                String soloRank = this.TidyRank(playerProfile[2],playerProfile[3]);
                String flexRank = this.TidyRank(playerProfile[4],playerProfile[5]);
                String tftRank = this.TidyRank(playerProfile[6],playerProfile[7]);
                String img = playerProfile[8];
                String champion[] = this.getSpecialization(playerProfile[9], playerProfile[11], playerProfile[13]);
                String champion1 = champion[0] + "\n" + playerProfile[10];
                String champion2 = champion[1] + "\n" + playerProfile[12];
                String champion3 = champion[2] + "\n" + playerProfile[14];

                LOLNameMapObject.put(event.getAuthor().getId(),messageSent[1]);
                event.getChannel().sendMessage(this.getPlayerProfile(event, messageSent[1], playerProfile[1], soloRank, flexRank, tftRank, img, time, champion1, champion2, champion3)).setActionRow(
                        Button.link("https://lol.moa.tw/summoner/show/" + messageSent[1], messageSent[1] + " 對戰紀錄"),
                        Button.primary("Refresh","刷新玩家資訊")
                ).queue();

                return;
            }

            if (contentRaw.contains("進入組隊房間")) {
                if (contentRaw.contains("https")) return;
                String url = contentRaw.replace(" ", "+");
                url = url.replace("\n", "%0D%0A");
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + "已為您查詢完畢").setActionRow(
                        Button.link("https://twlolstats.com/teammate/?teammates=" + url, "點此查看查詢結果")
                ).queue();

                return;
            }

            if (messageSent[0].equalsIgnoreCase("克制") || messageSent[0].equalsIgnoreCase("剋制")) {
                String name = messageSent[1];
                String LOLEnName = this.getLOLEnName(name);
                String html = this.getLOLName(LOLEnName);

                String[] restraint = this.getRestraint(html);
                MessageEmbed counterEmbed = this.getCounterEmbed(name, restraint[0], restraint[1], restraint[2], LOLEnName);
                event.getChannel().sendMessage(counterEmbed).setActionRow(
                        Button.link("https://www.ldcbot.cf/", "官方網站"),
                        Button.link("https://lihivip.com/yt0207", "訂閱我Youtube")
                ).queue();


                return;
            }
        }catch (StringIndexOutOfBoundsException mue){
            event.getChannel().sendMessage("查無此英雄。").queue();
        } catch (Exception mue) {
            mue.printStackTrace();
            event.getChannel().sendMessage("伺服器錯誤，請稍後在試。如有問題請聯繫: `小迪芯#3212` \n可自行上 https://twlolstats.com/ 檢查是否能正確連接").queue();
        }
    }

    //讀取玩家帳號ID
    private String getAccountId(GuildMessageReceivedEvent event, String playerName) throws IOException {
        String AccountIdHtml = null;
        try {
            String myURL = "https://acs-garena.leagueoflegends.com/v1/players?name=" + playerName + "&region=TW";
            Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
            AccountIdHtml = parse.html();
            int AccountId = AccountIdHtml.indexOf("tId");
            AccountIdHtml = AccountIdHtml.substring(AccountId + 5);
            int div = AccountIdHtml.indexOf("}");
            AccountIdHtml = AccountIdHtml.substring(0, div);

        } catch (HttpStatusException mue) {
            AccountIdHtml = "404";
        }

        return AccountIdHtml;
    }

    //讀取玩家資料API
    private String getSummonerDetail(GuildMessageReceivedEvent event, String AccountId) throws IOException {

        String myURL = "https://twlolstats.com/api/summoner-detail/" + AccountId;
        Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
        String SummonerHtml = parse.html();

        return SummonerHtml;
    }

    //整理API資料
    private String[] getPlayerProfile(GuildMessageReceivedEvent event, String SummonerDetail) throws IOException {
        String text = SummonerDetail;

        String[] messageSent = text.split("summoner");
        String update = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);

        messageSent = text.split("summonerLevel");
        String Level = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("profileIconId");
        String profileIconId = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("solo_tier");
        String SoloTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        SoloTier = this.unicodeToCn(SoloTier);
        if (SoloTier == "") {
            SoloTier = "未排位";
        }
        messageSent = text.split("solo_div");
        String SoloDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("solo_score");
        String SoloScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("flex_tier");
        String FlexTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        FlexTier = this.unicodeToCn(FlexTier);
        if (FlexTier == "") {
            FlexTier = "未排位";
        }
        messageSent = text.split("flex_div");
        String FlexDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("flex_score");
        String FlexScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("tft_tier");
        String tftTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        tftTier = this.unicodeToCn(tftTier);
        if (tftTier == "") {
            tftTier = "未排位";
        }
        messageSent = text.split("tft_div");
        String tftDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("tft_score");
        String tftScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion1");
        String champion1 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score1");
        String score1 = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion2");
        String champion2 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score2");
        String score2 = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion3");
        String champion3 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score3");
        String score3 = messageSent[1].substring(3, messageSent[1].indexOf("}"));

        text = update + " " + Level + " " + SoloTier + SoloDiv + " " + SoloScore + " " + FlexTier + FlexDiv + " " + FlexScore + " " + tftTier + tftDiv + " " + tftScore + " " + profileIconId + " " + champion1 + " " + score1 + " " + champion2 + " " + score2 + " " + champion3 + " " + score3;
        text = text.trim();
        return text.split(" ");
    }

    //發送玩家資訊訊息格式
    private MessageEmbed getPlayerProfile(GuildMessageReceivedEvent event, String name, String level, String solo, String flex, String tft, String image, String date,String champion1,String champion2,String champion3) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("LOL玩家資料查詢", null);
        eb.setColor(Color.red);
        eb.addField("玩家名稱", name, false);
        eb.addField("玩家等級", level, false);
        eb.addField("單雙積分", solo, true);
        eb.addField("彈性積分", flex, true);
        eb.addField("戰棋積分", tft, true);
        eb.addField("專精英雄No1", champion1, true);
        eb.addField("專精英雄No2", champion2, true);
        eb.addField("專精英雄No3", champion3, true);
        eb.setThumbnail("https://ddragon.leagueoflegends.com/cdn/11.16.1/img/profileicon/" + image + ".png");
        eb.setFooter("目前已加入 " + ldcbot.build.getGuilds().size() + " 個伺服器\n如果資料有顯示null可以按下刷新玩家資訊做刷新\n主機將在10/05過期，目前無法負擔主機費用，所以可能將停止服務\n該玩家資料最後更新日期: " + date, "https://i.imgur.com/oaH5uNh.jpg");
        return eb.build();
    }

    //整理排位內容
    private String TidyRank(String rank,String RankCount) throws IOException{
        String result;
        if (rank.contains("未排位")) {
            result = "未排位";
        } else if (rank.contains("菁英")) {
            result = "菁英" + " " + RankCount + "分";
        } else if (rank.contains("宗師")) {
            result = "宗師" + " " + RankCount + "分";
        } else if (rank.contains("大師")) {
            result = "大師" + " " + RankCount + "分";
        } else {
            result = rank + " " + RankCount + "分";
        }
    return result;
    }


    //發送克制訊息格式
    private MessageEmbed getCounterEmbed(String name, String t1, String t2, String t3,String image) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("克制【 " + name + " 】的英雄", null);
        eb.setColor(Color.CYAN);

        String[] t1Array = t1.split("#");
        String[] t2Array = t2.split("#");
        String[] t3Array = t3.split("#");

        t1Array[0] = this.getLOLTwName(t1Array[0]);
        t2Array[0] = this.getLOLTwName(t2Array[0]);
        t3Array[0] = this.getLOLTwName(t3Array[0]);

        eb.addField(t1Array[0], "勝率:" + t1Array[1], true);
        eb.addField(t2Array[0], "勝率:" + t2Array[1], true);
        eb.addField(t3Array[0], "勝率:" + t3Array[1], true);
        eb.setThumbnail("https://ddragon.leagueoflegends.com/cdn/11.16.1/img/champion/" + image + ".png");
        eb.setFooter("目前已加入 " + ldcbot.build.getGuilds().size() + " 個伺服器\n主機將在10/05過期，目前無法負擔主機費用，所以可能將停止服務\n勝率意思: 指 " + name + " 對於上方英雄的勝率!!");
        return eb.build();
    }

    //取得克制
    private String[] getRestraint(String text) {
        String restraint = text;

        if (restraint.contains(" Win Ratio")) {
            restraint = restraint.replace(" Win Ratio ", "#");
        }
        if (restraint.contains(" Counter")) {
            restraint = restraint.replace(" Counter", "");
        }

        return restraint.split(" ");
    }

    //獲取克制資料網站
    private String getLOLName(String name) throws IOException {
        Document parse = Jsoup.connect("https://tw.op.gg/champion/" + name + "/statistics/").get();
        Element table = parse.select("table").first();
        return table.text();
    }

    //轉換克制英雄為中文轉英文
    private String getLOLEnName(String LOLName) throws IOException {
        String myURL = "https://ddragon.leagueoflegends.com/cdn/11.16.1/data/zh_TW/champion.json";
        Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
        String html = parse.html();
        int count = html.indexOf("\"name\":\"" + LOLName + "\",");
        String LOLHtml = html.substring(count-50);
        String[] LOLEnName = LOLHtml.split("id");
        LOLName = LOLEnName[1].substring(3, LOLEnName[1].indexOf(",") - 1);

        return LOLName;
    }

    //轉換克制英雄英文轉中文
    private String getLOLTwName(String LOLName) throws IOException {
        String myURL = "https://ddragon.leagueoflegends.com/cdn/11.16.1/data/zh_TW/champion.json";
        Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
        String html = parse.html();
        int count = html.indexOf("\"id\":\"" + LOLName + "\",");
        String LOLHtml = html.substring(count+10);
        String[] LOLEnName = LOLHtml.split("name");
        LOLName = LOLEnName[1].substring(3, LOLEnName[1].indexOf(",") - 1);
        return LOLName;
    }
    //搜尋專精英雄
    private String[] getSpecialization(String champion1 ,String champion2,String champion3) throws IOException {
        String text=null;
        if(champion1.contains("null")||champion2.contains("null")||champion3.contains("null")){
            text = "null null null";
        }
        else {
            String myURL = "https://ddragon.leagueoflegends.com/cdn/11.16.1/data/zh_TW/champion.json";
            Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
            String html = parse.html();

            String[] messageSent = html.split("\"key\":\"" + champion1 + "\",");
            champion1 = messageSent[1].substring(8, messageSent[1].indexOf(",") - 1);

            messageSent = html.split("\"key\":\"" + champion2 + "\",");
            champion2 = messageSent[1].substring(8, messageSent[1].indexOf(",") - 1);

            messageSent = html.split("\"key\":\"" + champion3 + "\",");
            champion3 = messageSent[1].substring(8, messageSent[1].indexOf(",") - 1);

            text = champion1 + " " + champion2 + " " + champion3;
        }
        text = text.trim();
        return text.split(" ");
    }

    //解碼Unicode
    private static String unicodeToCn(String unicode) {
        String[] strs = unicode.split("\\\\u");
        String returnStr = "";
        for (int i = 1; i < strs.length; i++) {
            returnStr += (char) Integer.valueOf(strs[i], 16).intValue();
        }
        return returnStr;
    }

    //獲取更新時間
    private String GetAddTime(String time) throws IOException {
        String[] messageSent = time.split("T");
        String date = messageSent[1].substring(0, messageSent[1].indexOf(":"));
        int TwTime = Integer.valueOf(date);
        for (int i = 0; i < 8; i++) {
            TwTime++;
            if (TwTime == 24) {
                TwTime = 0;
            }
        }
        String stringValue = Integer.toString(TwTime);
        String NewTime = time.replaceAll(date,stringValue);
        NewTime = NewTime.replaceAll("T"," ");
        return NewTime;
    }

    //讀取玩家帳號ID Button
    private String getAccountIdButton(ButtonClickEvent event, String playerName) throws IOException {
        String AccountIdHtml = null;
        try {
            String myURL = "https://acs-garena.leagueoflegends.com/v1/players?name=" + playerName + "&region=TW";
            Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
            AccountIdHtml = parse.html();
            int AccountId = AccountIdHtml.indexOf("tId");
            AccountIdHtml = AccountIdHtml.substring(AccountId + 5);
            int div = AccountIdHtml.indexOf("}");
            AccountIdHtml = AccountIdHtml.substring(0, div);

        } catch (HttpStatusException mue) {
            AccountIdHtml = "404";
        }

        return AccountIdHtml;
    }

    //讀取玩家資料API Button
    private String getSummonerDetailButton(ButtonClickEvent event, String AccountId) throws IOException {
        String myURL = "https://twlolstats.com/api/summoner-detail/" + AccountId;
        Document parse = Jsoup.connect(myURL).ignoreContentType(true).get();
        String SummonerHtml = parse.html();

        return SummonerHtml;
    }

    //整理API資料 Button
    private String[] getPlayerProfileButton(ButtonClickEvent event, String SummonerDetail) throws IOException {
        String text = SummonerDetail;

        String[] messageSent = text.split("summoner");
        String update = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);

        messageSent = text.split("summonerLevel");
        String Level = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("profileIconId");
        String profileIconId = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("solo_tier");
        String SoloTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        SoloTier = this.unicodeToCn(SoloTier);
        if (SoloTier == "") {
            SoloTier = "未排位";
        }
        messageSent = text.split("solo_div");
        String SoloDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("solo_score");
        String SoloScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("flex_tier");
        String FlexTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        FlexTier = this.unicodeToCn(FlexTier);
        if (FlexTier == "") {
            FlexTier = "未排位";
        }
        messageSent = text.split("flex_div");
        String FlexDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("flex_score");
        String FlexScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("tft_tier");
        String tftTier = messageSent[1].substring(4, messageSent[1].indexOf(",") - 1);
        tftTier = this.unicodeToCn(tftTier);
        if (tftTier == "") {
            tftTier = "未排位";
        }
        messageSent = text.split("tft_div");
        String tftDiv = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("tft_score");
        String tftScore = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion1");
        String champion1 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score1");
        String score1 = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion2");
        String champion2 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score2");
        String score2 = messageSent[1].substring(3, messageSent[1].indexOf(","));

        messageSent = text.split("champion3");
        String champion3 = messageSent[1].substring(3, messageSent[1].indexOf(","));
        messageSent = text.split("score3");
        String score3 = messageSent[1].substring(3, messageSent[1].indexOf("}"));

        text = update + " " + Level + " " + SoloTier + SoloDiv + " " + SoloScore + " " + FlexTier + FlexDiv + " " + FlexScore + " " + tftTier + tftDiv + " " + tftScore + " " + profileIconId + " " + champion1 + " " + score1 + " " + champion2 + " " + score2 + " " + champion3 + " " + score3;
        text = text.trim();
        return text.split(" ");
    }

    //發送玩家New資訊訊息格式
    private MessageEmbed getPlayerProfileButton(ButtonClickEvent event, String name, String level, String solo, String flex, String tft, String image, String date,String champion1,String champion2,String champion3) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("LOL玩家資料查詢", null);
        eb.setColor(Color.red);
        eb.addField("玩家名稱", name, false);
        eb.addField("玩家等級", level, false);
        eb.addField("單雙積分", solo, true);
        eb.addField("彈性積分", flex, true);
        eb.addField("戰棋積分", tft, true);
        eb.addField("專精英雄No1", champion1, true);
        eb.addField("專精英雄No2", champion2, true);
        eb.addField("專精英雄No3", champion3, true);
        eb.setThumbnail("https://ddragon.leagueoflegends.com/cdn/11.16.1/img/profileicon/" + image + ".png");
        eb.setFooter("目前已加入 " + ldcbot.build.getGuilds().size() + " 個伺服器\n主機將在10/05過期，目前無法負擔主機費用，所以可能將停止服務\n感謝 twlolstats.com A準大大提供API\n該玩家資料最後更新日期: " + date, "https://i.imgur.com/oaH5uNh.jpg");
        return eb.build();
    }

    //觸發按鈕執行內容
    public void onButtonClick(ButtonClickEvent event) {
        if(event.getComponentId().equals("Refresh")) {
            try {
                String LOLName = LOLNameMapObject.get(event.getUser().getId());
                Connection connection = Jsoup.connect("https://twlolstats.com/summoner-match/?summoner="+LOLName);
                Document net = connection.get();

                String AccountIdTrue = this.getAccountIdButton(event, LOLName);

                String SummonerDetail = this.getSummonerDetailButton(event, AccountIdTrue);
                String[] playerProfile = this.getPlayerProfileButton(event, SummonerDetail);
                String time = this.GetAddTime(playerProfile[0]);
                String soloRank = this.TidyRank(playerProfile[2],playerProfile[3]);
                String flexRank = this.TidyRank(playerProfile[4],playerProfile[5]);
                String tftRank = this.TidyRank(playerProfile[6],playerProfile[7]);
                String img = playerProfile[8];
                String champion[] = this.getSpecialization(playerProfile[9], playerProfile[11], playerProfile[13]);
                String champion1 = champion[0] + "\n" + playerProfile[10];
                String champion2 = champion[1] + "\n" + playerProfile[12];
                String champion3 = champion[2] + "\n" + playerProfile[14];

                event.editMessageEmbeds(this.getPlayerProfileButton(event, LOLName, playerProfile[1], soloRank, flexRank, tftRank, img, time, champion1, champion2, champion3)).setActionRow(
                        Button.link("https://lol.moa.tw/summoner/show/" + LOLName, LOLName + " 對戰紀錄"),
                        Button.link("https://lihivip.com/yt0207", "訂閱我Youtube")
                ).queue();
            } catch (IOException e) {
                event.getChannel().sendMessage("刷新失敗").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }
}
