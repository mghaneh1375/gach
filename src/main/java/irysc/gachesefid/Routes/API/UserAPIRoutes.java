package irysc.gachesefid.Routes.API;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import irysc.gachesefid.Controllers.Finance.PayPing;
import irysc.gachesefid.Controllers.ManageUserController;
import irysc.gachesefid.Controllers.UserController;
import irysc.gachesefid.DB.Repository;
import irysc.gachesefid.Exception.*;
import irysc.gachesefid.Kavenegar.utils.PairValue;
import irysc.gachesefid.Models.AuthVia;
import irysc.gachesefid.Models.QuestionType;
import irysc.gachesefid.Models.Sex;
import irysc.gachesefid.Routes.Router;
import irysc.gachesefid.Security.JwtTokenFilter;
import irysc.gachesefid.Service.UserService;
import irysc.gachesefid.Utility.Authorization;
import irysc.gachesefid.Utility.Positive;
import irysc.gachesefid.Utility.Utility;
import irysc.gachesefid.Validator.JSONConstraint;
import irysc.gachesefid.Validator.ObjectIdConstraint;
import irysc.gachesefid.Validator.StrongJSONConstraint;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static irysc.gachesefid.Main.GachesefidApplication.*;
import static irysc.gachesefid.Utility.StaticValues.*;
import static irysc.gachesefid.Utility.Utility.*;


@Controller
@RequestMapping(path = "/api/user")
@Validated
public class UserAPIRoutes extends Router {


    @Autowired
    UserService userService;

    private static String token = "LYqPozxUPrpDVAxqBs7vXp1knTnEgEYUrnbpqKtggn1DHDQofCn=?aAdh6Mo/5I8qTcJSkAQppUCB/pzfD2G-0PNkl-R3Ub42BAv4QHFUlWgAfu6SwyRH!O3jUF426jw33EMCGJmYWSApEbCg?t4dRGowUX-ixRMUjqoCF/6s8IcyTQHtOYK9OD/xBGKjq/yzA/AEfz6xP3A7k-CBElUbTRU=3WyZaOjZnANV6EUE3PiK9g-5XKvos-qPP?Pcau=o8li6fDopR/L1ccV9SHlGEGxIMM7V5ZyKCKgxbc4J7lx0nyd0DCI?ddiWHXqAYbe2!oIZT-o4pEdJF2IVYj-jpyJI?VOwMrVR4P-4hrj2fyvW5fFhlgrpBVe6CpwGYdmcx7QxMyjwb5vAh!1tDM=jcVP/AL2a-JzbdtGhYCgkbxexHoZYFjV5goTGjTMXB?yQJrn!ABGC?BYV0B59NNi!0n/u/tbIqTant-FixPJ5otO4h6278ZHpiL9llIg9t0Rk3Sym=fnhEkE4GkfaM6jVTMaSC2iVc9jMg2ug2pXoK1NMaNA/LW77kiJTfrmnobzbPSK7fX84Klk7QtE53i-CdD=a=gB2NnWXKwqX7dic=S289OeaNlP705GZ9-6wvF!HvFTBtlyckzaAVXR2zjip7JLTvnwqrIWKd=P3Hs/!r3Rdmp7OTJgYw85Wq4dIX=Aa/lAc77n0wRyQ5AtaocYAYhstBoF2U96QTcUofNWOn1j6hJAUhboqd99L2dWykVtreRi7E7PYWNef1qrV/yUQbPKaNH4EKvppN03D3Rd1iCq1Kq8N=5ayyx=hOvrvqZ3EoT-x3RA-CY7m0i452xbKSP4nJLzT-/t!uB1/VlxDUoK8XdoGMZo0DMjUWbV8t5j9Kk/w7ta5vcPELszuqA-JkAnDlly?Tg=XBnbhJEyUTndjXrW3Y09BLn/hS7dcF2pU7Az6OleSnuMWGcBO2qPtwrUf2EgcwgPrqG4EwWzdx5IfeoKSbhC?BKi-mFcBV/bv!mnSZi7?BiLG?e1srXRJx?uY??lFXf3B2Lh-?R?d2BB7PV0x!UPgqwnRLCc8noaw0dqbrl6ab7U?Sl7CGlS2R4oeDIM=?jWBUL659cYQ/SdKJ-0xw9jWGWo?fx?qUzwbnrDgPvls2PdWot9ybfuuBJU7Kh2EgW?DbYBaU8MSfqnMLxTD1GKWVIGBFhCL-6n=oczCtLObrwz3j1g15ua2Igiuhf4s?LaRGtB1nFgD3q!5DRsye6IFK15PCS-TaQGFcNSXvh8woN4cOfSWhslDTKrlY5oiZN05qWu25?FI-2gWekEFKuZw3dMPIohxA2wlnx4ILk9Q6Y0iacefvPUm=zMHFot8jP5mFgZoLQRha-Q=6Xvl2ooUlqSdwMi83uoSfJgKAdMK2fa=NP?BT1vOdraDGZoXlQFmL4=2?FnN3Pa8iXVp/ZpNJ?DXnhGzsa624PkH5!HhsBS=YD?9qsX6y=XILOhuGrwub?sokmEVK0IKzd0AvHVpD1m96nlwXqO1KXmPjUodjBiZb/RQHH0?JzjydqV-AQmAdBWM-nel/In8wpRqkqsOHq!l5B6eNlCbqVjVB-?y8PHPorDL0?C1a9z0p7befPM?RaRD9Y127EWyqFgsrSzMJ-!chcC0CtKebR5uiopTQM=eI1jsiiXI5KNB7gIj95tGP=TcWy=HSr6mmfEhw1mUky-7!5m9ZUxYl9ghU6AvCQ!Yl?piMJZyYVVsrz07dELveO6pppGu0hEZ8qL-yfL2fGeR6MmCWdEsAkGg4cfTzQRp3?wfSAtziJ-49=U3XA7E?QS4hA?ZInmx!vTAFN?VvMDgg1LC9xzj8l2Dkzz?!=3TYHRLfAJVvgBBhS!WJkJSscQia-QEBAeKwe0347X6uwefT?0bo!9=iP0/vWgSF-DQ0=WgT/yARdniodePw!T-CAZlF1bfU/Z1O!RFJ2hunLYkdFWNyoepO?bFMCAV6h3hEhjSZkxtQy?fiV-BtX!Sh?-?jZBOa4Cecjj0eqaXehVB!G8WZEu";

    @GetMapping(value = "/gifts")
    @ResponseBody
    public void gifts() {

        AtomicInteger hits = new AtomicInteger();
        AtomicInteger miss = new AtomicInteger();

        HashMap<ObjectId, Document> gifts = new HashMap<>();

        userGiftRepository.find(and(
                eq("status", "finish")
        ), null).forEach(document -> {

            document.getList("gifts", Document.class).stream().filter(gift -> gift.getBoolean("selected").equals(true))
                    .findFirst().ifPresent(selectedGift -> {

                        if (!selectedGift.getString("label").contains("تخفیف"))
                            return;

                        boolean isPercent = selectedGift.getString("label").contains("%");
                        Document off = offcodeRepository.findOne(and(
                                eq("user_id", document.getObjectId("user_id")),
                                eq("type", isPercent ? "percent" : "value"),
                                eq("amount", Integer.parseInt(
                                                selectedGift.getString("label")
                                                        .replaceAll("[^0-9]", "")
                                        )
                                )), null
                        );

                        if (off != null) {

                            Document wantedGift;

                            if (gifts.containsKey(document.getObjectId("gift")))
                                wantedGift = gifts.get(document.getObjectId("gift"));
                            else {
                                wantedGift = giftRepository.findById(document.getObjectId("gift"));
                                gifts.put(document.getObjectId("gift"), wantedGift);
                            }

                            off.put("expire_at", wantedGift.getLong("expire_at"));
                            offcodeRepository.replaceOne(off.getObjectId("_id"), off);
                        } else {
                            miss.getAndIncrement();
                            System.out.println(selectedGift);
                        }

                    });
        });

        System.out.println(hits);
        System.out.println(miss);

    }


    @GetMapping(value = "/getMyCachedTokens")
    @ResponseBody
    public String getCachedTokens(HttpServletRequest request
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        getAdminPrivilegeUser(request);

        JSONArray jsonArray = new JSONArray();

        for (String p : passwords.keySet()) {

            jsonArray.put(new JSONObject()
                    .put("username", p)
                    .put("password", passwords.get(p))
            );
        }

        return generateSuccessMsg("data", jsonArray);
    }

    @GetMapping(value = "/fixQuiz")
    @ResponseBody
    public String fixQuiz() throws ParseException {

        if (1 == 1) {
            return "";
        }

        Document quiz = iryscQuizRepository.findById(new ObjectId("651a49d52cd29b3fcd1114a0"));
        List<Document> students = quiz.getList("students", Document.class);
        List<Document> questions = questionRepository.findByIds(
                quiz.get("questions", Document.class).getList("_ids", ObjectId.class), true
        );

        List<QuestionType> types = questions.stream().map(document -> document.getString("kind_question")).map(String::toUpperCase).map(QuestionType::valueOf)
                .collect(Collectors.toList());

        List<Object> answers = questions.stream().map(document -> document.get("answer")).collect(Collectors.toList());

        byte[] answersByte = new byte[0];
        int idx = -1;

        for (Document question : questions) {
            idx++;
            answersByte = irysc.gachesefid.Controllers.Quiz.Utility.addAnswerToByteArr(answersByte, question.getOrDefault("kind_question", "test").toString(),
                    types.get(idx).equals(QuestionType.TEST) ?
                            new PairValue(question.getInteger("choices_count"), question.get("answer")) :
                            question.get("answer")
            );
        }

        quiz.get("questions", Document.class).put("answers", answersByte);

        for (Document student : students) {

            if (student == null)
                continue;

            Object tmp = student.getOrDefault("answers", null);
            if (tmp != null)
                tmp = ((Binary) tmp).getData();
            else
                tmp = new byte[0];

            ArrayList<PairValue> oldStdAnswers = tmp == null ? new ArrayList<>() : irysc.gachesefid.Controllers.Quiz.Utility.getAnswers((byte[]) tmp);
            List<QuestionType> stdTypes = oldStdAnswers.stream().map(pairValue -> pairValue.getKey().toString())
                    .map(String::toUpperCase)
                    .map(QuestionType::valueOf).collect(Collectors.toList());

            if (stdTypes.size() != types.size()) {
                System.out.println("err1");
            } else {

                boolean hasErr = false;

                for (int i = 0; i < types.size(); i++) {
                    System.out.println(stdTypes.get(i) + " " + types.get(i));
                    if (!stdTypes.get(i).equals(types.get(i))) {
                        System.out.println("err2 in " + i);
                        hasErr = true;
                    }
                }

                if (!hasErr) continue;

                ArrayList<PairValue> newStdAnswers = new ArrayList<>();

                idx = -1;
                for (PairValue p : oldStdAnswers) {

                    idx++;
                    String stdAns = p.getValue() instanceof PairValue ?
                            ((PairValue) p.getValue()).getValue().toString() : p.getValue().toString();

                    Object stdAnsAfterFilter;
                    QuestionType type = types.get(idx);

                    if (stdAns.isEmpty()) {
                        if (type.equals(QuestionType.TEST)) {
                            newStdAnswers.add(new PairValue(
                                    type.getName(),
                                    new PairValue(((PairValue) p.getValue()).getKey(),
                                            0)
                            ));
                        } else if (type.equals(QuestionType.SHORT_ANSWER))
                            newStdAnswers.add(new PairValue(type.getName(), null));
                        else if (type.equals(QuestionType.MULTI_SENTENCE)) {

                            String s = "";
                            for (int z = 0; z < p.getValue().toString().length(); z++)
                                s += "_";

                            newStdAnswers.add(new PairValue(type.getName(), s.toCharArray()));
                        }
                        continue;
                    }

                    if (type.equals(QuestionType.TEST)) {
                        int s = NumberFormat.getInstance().parse(stdAns).intValue();
                        stdAnsAfterFilter = new PairValue(4, s);
                    } else if (type.equals(QuestionType.SHORT_ANSWER))
                        stdAnsAfterFilter = Double.parseDouble(stdAns);
                    else if (type.equals(QuestionType.MULTI_SENTENCE)) {

                        if (p.getValue().toString().length() != stdAns.length())
                            return JSON_NOT_VALID_PARAMS;

                        if (!stdAns.matches("^[01_]+$"))
                            return JSON_NOT_VALID_PARAMS;

                        stdAnsAfterFilter = stdAns.toCharArray();
                    } else
                        stdAnsAfterFilter = stdAns;

                    newStdAnswers.add(new PairValue(type.getName(), stdAnsAfterFilter));
                }

                student.put("answers", irysc.gachesefid.Controllers.Quiz.Utility.getStdAnswersByteArr(newStdAnswers));

            }

//                        System.out.println("err2 " + i);
//                        System.out.println("real ans " + answers.get(i));
//                        System.out.println("std ans " + ((stdAnswers.get(i).getValue() instanceof PairValue) ?
//                                ((PairValue)stdAnswers.get(i).getValue()).getValue() : stdAnswers.get(i).getValue()
//                        ));

        }

        iryscQuizRepository.replaceOne(quiz.getObjectId("_id"), quiz);
//                }
//            }
        return "";
    }

    @PostMapping(value = "/createOpenCardOff")
    @ResponseBody
    public String createOpenCardOff(HttpServletRequest request,
                                    @RequestBody @StrongJSONConstraint(
                                            params = {"amount", "mode"},
                                            paramsType = {Number.class, String.class}
                                    ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {

        Document user = getUser(request);
        JSONObject jsonObject = convertPersian(new JSONObject(jsonStr));
        String mode = jsonObject.getString("mode");

        if (!mode.equalsIgnoreCase("charge") && !mode.equalsIgnoreCase("coin"))
            return JSON_NOT_VALID_PARAMS;

        int amount;

        if (mode.equalsIgnoreCase("charge")) {
            double mainMoney = ((Number) (user.get("money"))).doubleValue();
            int money = (int) mainMoney;
            amount = jsonObject.getInt("amount");

            if (amount > money)
                return generateErr("حداکثر مقدار قابل تبدیل برای شما " + money + " می باشد.");
        } else {

            double coin = ((Number) (user.get("coin"))).doubleValue();

            if (jsonObject.getNumber("amount").doubleValue() > coin)
                return generateErr("حداکثر مقدار قابل تبدیل برای شما " + coin + " می باشد.");

            Document doc = Utility.getConfig();
            amount = (int) (doc.getInteger("coin_rate_coef") * jsonObject.getNumber("amount").doubleValue());
        }

        if (amount > 500000 || amount < 40000)
            return generateErr("حداکثر تخفیف ۵۰۰۰۰۰ و حداقل ۴۰۰۰۰ تومان می باشد.");

        while (true) {
            String code = "ir-" + Utility.simpleRandomString(3).replace("_", "") + Utility.getRandIntForGift(1000);
            JSONObject data = new JSONObject()
                    .put("name", System.currentTimeMillis() + "_" + user.getObjectId("_id").toString())
                    .put("code", code)
                    .put("discount", amount)
                    .put("type", "F")
                    .put("token", token);

            try {

                HttpResponse<String> jsonResponse = Unirest.post("https://shop.irysc.com/index.php?route=extension/total/coupon/add_coupon_api")
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .body(data).asString();

                if (jsonResponse.getStatus() == 200) {
                    String res = jsonResponse.getBody();
                    if (res.equals("ok")) {

                        if (mode.equalsIgnoreCase("charge")) {
                            double mainMoney = ((Number) (user.get("money"))).doubleValue();
                            user.put("money", ((mainMoney - jsonObject.getInt("amount")) * 100.0) / 100.0);
                        } else {
                            double coin = ((Number) (user.get("coin"))).doubleValue();
                            user.put("coin", ((coin - jsonObject.getNumber("amount").doubleValue()) * 100.0) / 100.0);
                        }

                        userRepository.replaceOne(user.getObjectId("_id"), user);
                        return generateSuccessMsg("data", code);
                    }

                    if (!res.equals("nok4")) {
                        System.out.println(res);
                        return JSON_NOT_UNKNOWN;
                    }
                }

            } catch (UnirestException e) {
                return generateErr(e.getMessage());
            }
        }


    }

    @PostMapping(value = "clearCache/{table}")
    @ResponseBody
    public String clearCache(HttpServletRequest request,
                             @PathVariable @NotBlank String table)
            throws NotAccessException, UnAuthException, NotActivateAccountException {
        getAdminPrivilegeUserVoid(request);
        Repository.clearCache(table);
        return JSON_OK;
    }

    @GetMapping(value = {"/fetchUser", "/fetchUser/{userId}"})
    @ResponseBody
    public String fetchUser(HttpServletRequest request,
                            @PathVariable(required = false) String userId)
            throws NotActivateAccountException, UnAuthException, InvalidFieldsException, NotCompleteAccountException {
        Document user = (Document) getUserWithSchoolAccess(request, false, false, userId).get("user");
        return generateSuccessMsg("user",
                UserController.isAuth(user)
        );
    }

    @GetMapping(value = "/getInfo")
    @ResponseBody
    public String getInfo(HttpServletRequest request)
            throws NotCompleteAccountException, NotActivateAccountException, UnAuthException {
        return ManageUserController.fetchUser(getUser(request), null, false);
    }

    @PostMapping(value = "/signIn")
    @ResponseBody
    public String signIn(@RequestBody @JSONConstraint(
            params = {"username", "password"}
    ) @NotBlank String jsonStr) {

        try {
            JSONObject jsonObject =
                    Utility.convertPersian(new JSONObject(jsonStr));

            return userService.signIn(
                    jsonObject.get("username").toString().toLowerCase(),
                    jsonObject.get("password").toString(), !DEV_MODE
            );

        } catch (NotActivateAccountException x) {
            return Utility.generateErr("حساب کاربری شما هنوز فعال نشده است.");
        } catch (Exception x) {
            return Utility.generateErr("نام کاربری و یا رمزعبور اشتباه است.");
        }
    }

    @PostMapping(value = "/logout")
    @ResponseBody
    public String logout(HttpServletRequest request)
            throws NotActivateAccountException, UnAuthException {

        getUserWithOutCheckCompleteness(request);

        try {
            String token = request.getHeader("Authorization");
            userService.logout(token);
            JwtTokenFilter.removeTokenFromCache(token.replace("Bearer ", ""));
            return JSON_OK;
        } catch (Exception x) {
            System.out.println(x.getMessage());
        }

        return JSON_NOT_VALID_TOKEN;
    }

    @PutMapping(value = "/aboutMe")
    @ResponseBody
    public String aboutMe(HttpServletRequest request,
                          @RequestBody @StrongJSONConstraint(
                                  params = {
                                          "adviceAboutMe", "wantToTeach"
                                  },
                                  paramsType = {
                                          String.class, Boolean.class
                                  },
                                  optionals = {
                                          "teachVideoLink", "adviceVideoLink",
                                          "defaultTeachPrice", "teachAboutMe"
                                  },
                                  optionalsType = {
                                          String.class, String.class,
                                          Positive.class, String.class
                                  }
                          ) @NotBlank String json
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return UserController.setAboutMe(getAdvisorUser(request), new JSONObject(json));
    }

    @GetMapping(value = "getMyFields")
    @ResponseBody
    public String getMyFields(HttpServletRequest request) throws NotAccessException, UnAuthException, NotActivateAccountException {
        return UserController.getMyFields(getAdvisorUser(request));
    }

    @PutMapping(value = "setMyFields")
    @ResponseBody
    public String setMyFields(
            HttpServletRequest request,
            @RequestBody @StrongJSONConstraint(
                    params = {},
                    paramsType = {},
                    optionals = {
                            "lessons", "grades",
                            "branches"
                    },
                    optionalsType = {
                            JSONArray.class, JSONArray.class,
                            JSONArray.class
                    }
            ) @NotBlank String jsonStr
    ) throws NotCompleteAccountException, UnAuthException, NotActivateAccountException {
        JSONObject jsonObject = new JSONObject(jsonStr);
        return UserController.setMyFields(
                getUser(request),
                jsonObject.has("lessons") ? jsonObject.getJSONArray("lessons") : null,
                jsonObject.has("grades") ? jsonObject.getJSONArray("grades") : null,
                jsonObject.has("branches") ? jsonObject.getJSONArray("branches") : null
        );
    }


    @PostMapping(value = "/signUp")
    @ResponseBody
    public String signUp(@RequestBody @StrongJSONConstraint(
            params = {
                    "password", "username",
                    "firstName", "lastName",
                    "NID", "authVia"
            },
            paramsType = {
                    String.class, String.class,
                    String.class, String.class,
                    String.class, AuthVia.class
            }
    ) @NotBlank String json) {

        try {
            JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

            if (!Utility.isValidPassword(jsonObject.getString("password")))
                return generateErr("رمزعبور باید حداقل 6 کاراکتر باشد.");

            jsonObject.put("password", userService.getEncPass(jsonObject.getString("password")));

            return UserController.signUp(jsonObject);
        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID;
    }

    @PostMapping(value = {"/updateInfo", "/updateInfo/{userId}"})
    @ResponseBody
    public String updateInfo(HttpServletRequest request,
                             @PathVariable(required = false) String userId,
                             @RequestBody @StrongJSONConstraint(
                                     params = {
                                             "sex", "cityId",
                                             "firstName", "lastName",
                                             "NID"
                                     },
                                     paramsType = {
                                             Sex.class, ObjectId.class,
                                             String.class, String.class,
                                             String.class
                                     },
                                     optionals = {
                                             "branches", "schoolId", "gradeId", "birthDay"
                                     },
                                     optionalsType = {
                                             JSONArray.class, ObjectId.class,
                                             ObjectId.class, Long.class
                                     }
                             ) String json
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.updateInfo(convertPersian(new JSONObject(json)),
                (Document) getUserWithSchoolAccess(request, false, false, userId).get("user")
        );
    }


    @PutMapping(value = {"/blockNotif", "/blockNotif/{studentId}"})
    @ResponseBody
    public String blockNotif(HttpServletRequest request,
                             @PathVariable(required = false) String studentId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.blockNotif((Document) getUserWithSchoolAccess(request, false, false, studentId).get("user"));
    }


    @PostMapping(value = "/resendCode")
    @ResponseBody
    public String resendCode(@RequestBody @JSONConstraint(params = {"token", "username"}) String json) {
        return UserController.resend(convertPersian(new JSONObject(json)));
    }

    @PostMapping(value = "/activate")
    @ResponseBody
    public String activate(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code"},
            paramsType = {String.class, String.class, Positive.class}
    ) String json) {

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

        int code = jsonObject.getInt("code");
        if (code < 100000 || code > 999990)
            return JSON_NOT_VALID_PARAMS;

        try {

            String password = UserController.activate(code,
                    jsonObject.getString("token"),
                    jsonObject.getString("NID")
            );

            return userService.signIn(jsonObject.getString("NID"),
                    password, false);

        } catch (Exception e) {
            return generateErr(e.getMessage());
        }
    }

    @GetMapping(value = "/getRoleForms")
    @ResponseBody
    public String getRoleForms() {
        return UserController.getRoleForms();
    }

    @PostMapping(value = {"/sendRoleForm", "/sendRoleForm/{userId}"})
    @ResponseBody
    public String sendRoleForm(HttpServletRequest request,
                               @PathVariable(required = false) String userId,
                               @RequestBody @StrongJSONConstraint(
                                       params = {"role"},
                                       paramsType = {String.class},
                                       optionals = {
                                               "name", "tel", "state",
                                               "workYear", "workSchools", "universeField",
                                               "bio", "address", "managerName", "schoolSex",
                                               "kindSchool", "invitationCode", "workLessons"
                                       },
                                       optionalsType = {
                                               String.class, String.class, String.class,
                                               Positive.class, String.class, String.class,
                                               String.class, String.class, String.class,
                                               String.class, String.class, String.class,
                                               String.class

                                       }
                               ) String json
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.setRole(
                (Document) getUserWithAdminAccess(request, false, false, userId).get("user"),
                new JSONObject(json)
        );
    }

    @PutMapping(value = "/setIntroducer")
    @ResponseBody
    public String setIntroducer(HttpServletRequest request,
                                @RequestBody @StrongJSONConstraint(
                                        params = {"invitationCode"},
                                        paramsType = {String.class}
                                ) String json
    ) throws UnAuthException, NotActivateAccountException {
        return UserController.setIntroducer(
                getUserWithOutCheckCompleteness(request),
                new JSONObject(json).getString("invitationCode")
        );
    }

    @GetMapping(value = "/whichKindOfAuthIsAvailable")
    @ResponseBody
    public String whichKindOfAuthIsAvailable(@RequestParam @Digits(integer = 10, fraction = 0) String NID) {
        return UserController.whichKindOfAuthIsAvailable(NID);
    }

    @PostMapping(value = "/forgetPassword")
    @ResponseBody
    public String forgetPassword(@RequestBody @JSONConstraint(params = {"NID", "authVia"}) String json) {
        return UserController.forgetPass(convertPersian(new JSONObject(json)));
    }

    @PostMapping(value = "/checkCode")
    @ResponseBody
    public String checkCode(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code"},
            paramsType = {String.class, String.class, Positive.class}
    ) String json) {

        JSONObject jsonObject = convertPersian(new JSONObject(json));

        Document doc = activationRepository.findOne(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.get("NID").toString()),
                        eq("code", jsonObject.getInt("code"))
                ), new BasicDBObject("_id", 1)
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        return JSON_OK;
    }

    @PostMapping(value = "/resetPassword")
    @ResponseBody
    public String resetPassword(@RequestBody @StrongJSONConstraint(
            params = {"token", "NID", "code", "newPass", "rNewPass"},
            paramsType = {String.class, String.class, Positive.class,
                    String.class, String.class}
    ) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        if (!Utility.validationNationalCode(jsonObject.getString("NID")))
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getString("token").length() != 20)
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getInt("code") < 100000 || jsonObject.getInt("code") > 999999)
            return Utility.generateErr("کد وارد شده معتبر نمی باشد.");

        if (!jsonObject.getString("newPass").equals(jsonObject.getString("rNewPass")))
            return Utility.generateErr("رمزجدید و تکرار آن یکسان نیستند.");

//        if (!Utility.isValidPassword(jsonObject.getString("newPass")))
//            return Utility.generateErr("رمزجدید انتخاب شده قوی نیست.");

        Document doc = activationRepository.findOneAndDelete(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", jsonObject.getString("NID")),
                        eq("code", jsonObject.getInt("code"))
                )
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC_LONG)
            return Utility.generateErr("توکن موردنظر منقضی شده است.");

        userRepository.updateOne(
                eq("NID", jsonObject.getString("NID")),
                set("password", userService.getEncPass(doc.getString("username"),
                        jsonObject.getString("newPass")))
        );

        return JSON_OK;
    }

    @PostMapping(value = "/setNewUsername")
    @ResponseBody
    public String setNewUsername(HttpServletRequest request,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {"token", "code"},
                                         paramsType = {String.class, Positive.class},
                                         optionals = {"NID"},
                                         optionalsType = {String.class}
                                 ) String json
    ) throws UnAuthException, NotActivateAccountException {

        Document user = getUserWithOutCheckCompleteness(request);

        JSONObject jsonObject = Utility.convertPersian(new JSONObject(json));

        if (jsonObject.getString("token").length() != 20)
            return JSON_NOT_VALID_PARAMS;

        if (jsonObject.getInt("code") < 100000 || jsonObject.getInt("code") > 999999)
            return Utility.generateErr("کد وارد شده معتبر نمی باشد.");

        Document doc = activationRepository.findOneAndDelete(
                and(
                        eq("token", jsonObject.getString("token")),
                        eq("username", user.getString("NID")),
                        eq("code", jsonObject.getInt("code"))
                )
        );

        if (doc == null)
            return generateErr("کد وارد شده معتبر نیست و یا توکن شما منقضی شده است.");

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return Utility.generateErr("توکن موردنظر منقضی شده است.");

        String phoneOrMail = doc.getString("phone_or_mail");

        if (doc.getString("auth_via").equals(AuthVia.SMS.getName())) {

            if (user.containsKey("phone"))
                userService.deleteFromCache(user.getString("phone"));

            user.put("phone", phoneOrMail);
        } else {

            if (user.containsKey("mail"))
                userService.deleteFromCache(user.getString("mail"));

            user.put("mail", phoneOrMail);
        }

        userRepository.replaceOne(user.getObjectId("_id"), user);
//        logout(request);

        return JSON_OK;
    }

    @PostMapping(value = {"/updateUsername", "/updateUsername/{userId}"})
    @ResponseBody
    public String updateUsername(HttpServletRequest request,
                                 @PathVariable(required = false) String userId,
                                 @RequestBody @JSONConstraint(params = {"mode", "username"})
                                 @NotBlank String json

    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {

        Document doc = getUserWithSchoolAccess(request, false, false, userId);

        if (doc.getBoolean("isAdmin"))
            return UserController.forceUpdateUsername(
                    (Document) doc.get("user"), convertPersian(new JSONObject(json))
            );

        return UserController.updateUsername(
                ((Document) doc.get("user")).getString("NID"),
                convertPersian(new JSONObject(json))
        );
    }

    @GetMapping(value = "/doChangeMail/{link}")
    @ResponseBody
    public String doChangeMail(HttpServletRequest request,
                               @PathVariable @NotBlank String link)
            throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return UserController.doChangeMail(getUser(request), link);
    }

    @PostMapping(value = "/confirmForgetPassword")
    @ResponseBody
    public String confirmForgetPassword(@RequestBody @JSONConstraint(params = {"token", "mail", "code"}) String json) {

        JSONObject jsonObject = new JSONObject(json);
        Utility.convertPersian(jsonObject);

        jsonObject.put("mail", jsonObject.getString("mail").toLowerCase());

        if (jsonObject.getString("token").length() != 20)
            return new JSONObject().put("status", "nok").put("msg", "invalid token").toString();

        if (jsonObject.getInt("code") < 10000 || jsonObject.getInt("code") > 99999)
            return new JSONObject().put("status", "nok").put("msg", "invalid code").toString();

        if (!Utility.isValidMail(jsonObject.getString("mail")))
            return new JSONObject().put("status", "nok").put("msg", "mail is incorrect").toString();

        Document doc = activationRepository.findOne(and(
                        eq("token", jsonObject.getString("token")),
                        eq("mail", jsonObject.getString("mail")),
                        eq("code", jsonObject.getInt("code")))
                , null);

        if (doc == null)
            return JSON_NOT_VALID_TOKEN;

        if (doc.getLong("created_at") < System.currentTimeMillis() - SMS_VALIDATION_EXPIRATION_MSEC)
            return new JSONObject().put("status", "nok").put("msg", "token has been expired").toString();

        return JSON_OK;
    }

    @PostMapping(value = {"/changePassword", "/changePassword/{userId}"})
    @ResponseBody
    public String changePassword(HttpServletRequest request,
                                 @PathVariable(required = false) String userId,
                                 @RequestBody @StrongJSONConstraint(
                                         params = {
                                                 "oldPass", "newPass", "confirmNewPass"
                                         },
                                         paramsType = {
                                                 String.class, String.class, String.class
                                         }) String jsonStr
    ) throws NotActivateAccountException, UnAuthException, NotCompleteAccountException, InvalidFieldsException {

        Document doc = getUserWithSchoolAccess(request, false, false, userId);
        Document user = (Document) doc.get("user");
        boolean isAdmin = doc.getBoolean("isAdmin");

        try {
            JSONObject jsonObject = new JSONObject(jsonStr);

            String newPass = jsonObject.get("newPass").toString();
            String confirmNewPass = jsonObject.get("confirmNewPass").toString();
            String oldPassword = jsonObject.get("oldPass").toString();

            if (!newPass.equals(confirmNewPass))
                return generateErr("رمزعبور جدید و تکرار آن یکسان نیستند.");

//            if (!Utility.isValidPassword(newPass))
//                return generateErr("رمزعبور جدید قوی نیست.");

            newPass = Utility.convertPersianDigits(newPass);
            if (!isAdmin) {
                oldPassword = Utility.convertPersianDigits(oldPassword);

                if (!userService.isOldPassCorrect(oldPassword, user.getString("password")))
                    return Utility.generateErr("رمزعبور وارد شده صحیح نیست.");
            }

            userService.deleteFromCache(user.getString("NID"));

            if (user.containsKey("phone") && !user.getString("phone").isEmpty())
                userService.deleteFromCache(user.getString("phone"));

            if (user.containsKey("mail") && !user.getString("mail").isEmpty())
                userService.deleteFromCache(user.getString("mail"));

            newPass = userService.getEncPass(newPass);
            user.put("password", newPass);

            userRepository.updateOne(user.getObjectId("_id"), set("password", newPass));
            logout(request);

            return JSON_OK;

        } catch (Exception x) {
            printException(x);
        }

        return JSON_NOT_VALID_PARAMS;
    }


    @PostMapping(value = {"/setPic", "/setPic/{userId}"})
    @ResponseBody
    public String setPic(HttpServletRequest request,
                         @RequestBody MultipartFile file,
                         @PathVariable(required = false) String userId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {

        if (file == null)
            return JSON_NOT_VALID_PARAMS;

        Document user = (Document) getUserWithAdminAccess(request, false, true, userId).get("user");

        if (UserController.setPic(file, user))
            return generateSuccessMsg("file", "");

        return JSON_NOT_UNKNOWN;
    }

    @PutMapping(value = {"/setAvatar/{avatarId}", "/setAvatar/{avatarId}/{userId}"})
    @ResponseBody
    public String setAvatar(HttpServletRequest request,
                            @PathVariable @ObjectIdConstraint ObjectId avatarId,
                            @PathVariable(required = false) String userId
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException, InvalidFieldsException {
        return UserController.setAvatar((
                        Document) getUserWithSchoolAccess(request, false, false, userId).get("user"),
                avatarId
        );
    }


    @GetMapping(value = "/myTransactions")
    @ResponseBody
    public String myTransactions(HttpServletRequest request
    ) throws UnAuthException, NotActivateAccountException, NotCompleteAccountException {
        return PayPing.myTransactions(getUser(request).getObjectId("_id"));
    }

    @GetMapping(value = "/getEducationalHistory/{userId}")
    @ResponseBody
    public String getEducationalHistory(HttpServletRequest request,
                                        @PathVariable @ObjectIdConstraint ObjectId userId
    ) throws NotAccessException, UnAuthException, NotActivateAccountException {

        Document user = getPrivilegeUser(request);
        if (!Authorization.hasWeakAccessToThisStudent(userId, user.getObjectId("_id")))
            return JSON_NOT_ACCESS;

        return UserController.getEducationalHistory(userId);
    }

//    @GetMapping(value = "/myTransaction/{referenceId}")
//    @ResponseBody
//    public String myTransaction(HttpServletRequest request,
//                                @PathVariable @ObjectIdConstraint ObjectId referenceId
//    ) throws UnAuthException, NotActivateAccountException, NotAccessException, NotCompleteAccountException {
//        return PayPing.myTransaction(getStudentUser(request).getObjectId("_id"), referenceId);
//    }

}
