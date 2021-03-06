package cn.seu.weme.service.impl;

import cn.seu.weme.common.result.ResponseInfo;
import cn.seu.weme.common.result.ResultInfo;
import cn.seu.weme.common.result.ResultUtil;
import cn.seu.weme.common.utils.*;
import cn.seu.weme.dao.*;
import cn.seu.weme.dto.PersonImageVo;
import cn.seu.weme.dto.SchoolInfoVo;
import cn.seu.weme.dto.UserInfoVo;
import cn.seu.weme.dto.UserVo;
import cn.seu.weme.dto.old.ActivityVo;
import cn.seu.weme.entity.*;
import cn.seu.weme.service.UserService;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

/**
 * Created by LCN on 2016-12-17.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private ActivityDao activityDao;

    @Autowired
    private UserImageDao userImageDao;

    @Autowired
    private CheckMsgDao checkMsgDao;

    @Autowired
    private AvatarVoiceDao avatarVoiceDao;

    @Autowired
    private FollowRelationDao followRelationDao;

    @Autowired
    private UserAttendActivityRelationDao userAttendActivityRelationDao;


    @Autowired
    private UserLikeActivityRelationDao userLikeActivityRelationDao;

    @Autowired
    private ModelMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MessageSourceHelper messageSourceHelper;


    @Override
    public ResultInfo attendActityV2(Long userId, Long activityId) {
//        User user = userDao.findOne(userId);
//        Activity activity = activityDao.findOne(activityId);
//        activity.getAttendUsers().add(user);
//        activityDao.save(activity);
        return ResultUtil.createSuccess("参加活动成功");
    }

    @Override
    public ResponseInfo attendActivity(String token, Long activityId) {
        User user = userDao.findByToken(token);
        Activity activity = activityDao.findOne(activityId);
        UserAttendActivityRelation userAttendActivityRelation = new UserAttendActivityRelation(user, activity);
        userAttendActivityRelationDao.save(userAttendActivityRelation);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public ResponseInfo unAttendActivity(String token, Long activityId) {
        User user = userDao.findByToken(token);

        javax.persistence.Query query = entityManager.createNativeQuery("DELETE FROM t_user_attend_activity_relation WHERE user_id =?1 AND activity_id = ?2");
        query.setParameter(1, user.getId());
        query.setParameter(2, activityId);
        query.executeUpdate();


        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }


    @Override
    public ResultInfo attendActivity2(Long userId, Long activityId) {

        userDao.attendActivity(userId, activityId);
        return ResultUtil.createSuccess("参加活动成功");
    }

    @Override
    public ResultInfo likeActity(Long userId, Long activityId) {
//        User user = userDao.findOne(userId);
//        Activity activity = activityDao.findOne(activityId);
//        activity.getLikeUsers().add(user);
//        activityDao.save(activity);
        return ResultUtil.createSuccess("喜欢活动成功");
    }

    @Override
    public ResultInfo followUser(Long followerId, Long followedId) {
        javax.persistence.Query query = entityManager.createNativeQuery("insert into t_follower_followed(follower_id,followed_id) VALUES(?1,?2)");
        query.setParameter(1, followerId);
        query.setParameter(2, followedId);
        query.executeUpdate();
        return ResultUtil.createSuccess("关注成功");
    }

    @Override
    public ResultInfo getFollowUsers(Long userId) {
//        Set<User> users = userDao.findOne(userId).getFolloweds();
//        Set<UserVo> userVos = new HashSet<>();
//        users.forEach(args -> userVos.add(mapper.map(args, UserVo.class)));
//        return ResultUtil.createSuccess("关注自己的人", userVos);
        return null;
    }

    @Override
    public ResultInfo getFollowedUsers(Long userId) {
//        Set<User> users = userDao.findOne(userId).getFollowers();
//        Set<UserVo> userVos = new HashSet<>();
//        users.forEach(args -> userVos.add(mapper.map(args, UserVo.class)));
//        return ResultUtil.createSuccess("自己的关注者", userVos);
        return null;
    }

    @Override
    public ResultInfo uploadImage(Long userId, PersonImageVo personImageVo) {
        User user = userDao.findOne(userId);
        UserImage userImage = mapper.map(personImageVo, UserImage.class);
        userImage.setUser(user);

        userImageDao.save(userImage);
        return ResultUtil.createSuccess("保存图片成功");
    }


    @Override
    public ResultInfo registerV2(String phone, String code, String password) {

        if (Strings.isNullOrEmpty(phone) || Strings.isNullOrEmpty(code) || Strings.isNullOrEmpty(password)) {
            return ResultUtil.createFail(messageSourceHelper.getMessage("100"));
        }

        User user = userDao.findByPhone(phone);
        if (user != null) {
            return ResultUtil.createFail(messageSourceHelper.getMessage("101"));
        }

        CheckMsg checkMsg = checkMsgDao.findByPhone(phone);
        if (checkMsg == null || !checkMsg.getCode().equals(code)) {
            return ResultUtil.createFail(messageSourceHelper.getMessage("102"));
        }

        if (Minutes.minutesBetween(new DateTime(), new DateTime(checkMsg.getTimestamp())).getMinutes() > 5) {
            return ResultUtil.createFail("验证码超时!");
        }


        String token = TokenProcessor.generateToken(phone + code + password);
        String salt = CryptoUtils.getSalt();
        String hashedPassword = CryptoUtils.getHash(password, salt);
        User newUser = new User(phone, hashedPassword, salt, token);
        userDao.save(newUser);

        Map data = new HashMap<>();
        data.put("token", token);

        return ResultUtil.createSuccess(messageSourceHelper.getMessage("103"), data);
    }

    @Override
    public ResponseInfo register(String phone, String code, String password) {
        ResponseInfo responseInfo = new ResponseInfo();
        if (Strings.isNullOrEmpty(phone) || Strings.isNullOrEmpty(code) || Strings.isNullOrEmpty(password)) {
            responseInfo.setState("fail");
            responseInfo.setReason("参数错误");
            return responseInfo;
        }

        if (userDao.findByPhone(phone) != null) {
            responseInfo.setReason("该手机号已经注册");
            responseInfo.setState("fail");
            return responseInfo;
        }

        if (!checkMsgCode(phone, code)) {
            responseInfo.setState("fail");
            responseInfo.setReason("验证码无效");
            return responseInfo;
        }

        String token = JWTUtils.generateToken(phone + code + password);
        String salt = CryptoUtils.getSalt();
        String hashedPassword = CryptoUtils.getHash(password, salt);
        User user = new User(phone, hashedPassword, salt, token);
        user.setUsername(phone);
        userDao.save(user);
        responseInfo.setState("successful");
        responseInfo.setReason("");
        responseInfo.setToken(user.getToken());
        responseInfo.setId(user.getId());
        return responseInfo;
    }

    @Override
    public ResultInfo resetPasswordV2(String newPassword, String phone, String code) {
        User user = userDao.findByPhone(phone);
        if (user == null) {
            return ResultUtil.createFail("该用户还未注册!");
        }
        CheckMsg checkMsg = checkMsgDao.findByPhone(phone);
        if (checkMsg == null || !checkMsg.getCode().equals(code)) {
            return ResultUtil.createFail("验证码错误!");
        }

        if (Minutes.minutesBetween(new DateTime(), new DateTime(checkMsg.getTimestamp())).getMinutes() > 5) {
            return ResultUtil.createFail("验证码超时!");
        }

        String token = JWTUtils.generateToken(phone + code + newPassword);

        String salt = CryptoUtils.getSalt();
        String hashedPassword = CryptoUtils.getHash(newPassword, salt);
        user.setToken(token);
        user.setPassword(hashedPassword);
        user.setSalt(salt);
        userDao.save(user);
        return ResultUtil.createFail("重置密码成功!");
    }

    @Override
    public ResponseInfo resetPassword(String newPassword, String phone, String code) {
        ResponseInfo responseInfo = new ResponseInfo();
        User user = userDao.findByPhone(phone);
        if (user == null) {
            responseInfo.setState("fail");
            responseInfo.setReason("该手机号尚未被注册");
            return responseInfo;
        }

        if (!checkMsgCode(phone, code)) {
            responseInfo.setState("fail");
            responseInfo.setReason("验证码无效");
            return responseInfo;
        }

        String token = JWTUtils.generateToken(phone + code + newPassword);
        String salt = CryptoUtils.getSalt();
        String hashedPassword = CryptoUtils.getHash(newPassword, salt);
        user.setToken(token);
        user.setPassword(hashedPassword);
        user.setSalt(salt);
        userDao.save(user);

        responseInfo.setState("successful");
        responseInfo.setReason("");
        responseInfo.setToken(user.getToken());
        responseInfo.setId(user.getId());
        return responseInfo;
    }

    @Override
    public ResultInfo loginV2(String username, String password) {
        //用户名保证唯一
        User user = userDao.findByUsername(username);
        if (user == null) {
            return ResultUtil.createFail("用户名密码错误");
        }

        if (!CryptoUtils.verify(user.getPassword(), password, user.getSalt())) {
            return ResultUtil.createFail("用户名密码错误");
        }

        String token = user.getToken();

        return ResultUtil.createSuccess("用户登录成功!", token);
    }

    @Override
    public ResponseInfo login(String username, String password) {
        ResponseInfo responseInfo = new ResponseInfo();
        User user = userDao.findByUsername(username);
        if (user == null) {
            responseInfo.setState("fail");
            responseInfo.setReason("用户名密码错误");
            return responseInfo;
        }
        if (!CryptoUtils.verify(user.getPassword(), password, user.getSalt())) {
            responseInfo.setState("fail");
            responseInfo.setReason("用户名密码错误");
            return responseInfo;
        }

        responseInfo.setState("successful");
        responseInfo.setReason("");
        responseInfo.setId(user.getId());
        responseInfo.setToken(user.getToken());
        responseInfo.setGender(user.getGender());
        return responseInfo;
    }

    @Override
    public ResponseInfo sendSmsCode(String phone, int type) {
        ResponseInfo responseInfo = new ResponseInfo();
        boolean success = false;

        if (type == 1) {
            if (userDao.findByPhone(phone) != null) {
                responseInfo.setState("fail");
                responseInfo.setReason("该手机号已经注册");
                return responseInfo;
            }
        } else if (type == 2) {
            if (userDao.findByPhone(phone) == null) {
                responseInfo.setState("fail");
                responseInfo.setReason("该手机号尚未注册");
                return responseInfo;
            }
        }

        String code = RandUtils.getRandomString(6);
        success = SmsUtils.sendSmsCodeByType(phone, code, type);
        if (!success) {
            responseInfo.setState("fail");
            responseInfo.setReason("验证码发送失败");
            return responseInfo;
        }

        CheckMsg checkMsg = checkMsgDao.findByPhone(phone);
        if (checkMsg == null) {
            checkMsg = new CheckMsg(phone, code);
        } else {
            checkMsg.setCode(code);
            checkMsg.setTimestamp(new Date());
        }
        checkMsgDao.save(checkMsg);
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }


    @Override
    public ResultInfo addUser(UserVo userVo) {
        User user = mapper.map(userVo, User.class);
        userDao.save(user);
        return ResultUtil.createSuccess("创建用户成功");
    }

    @Override
    public ResultInfo getUserById(Long id) {
        User user = userDao.findOne(id);
        if (user == null) {
            return ResultUtil.createFail("没有此用户");
        }
        UserVo userVo = mapper.map(user, UserVo.class);
        return ResultUtil.createSuccess("用户信息", userVo);
    }

    @Override
    public ResultInfo updateUserV2(UserVo userVo) {
        User user = userDao.findOne(userVo.getId());
        if (user == null) {
            return ResultUtil.createFail("没有此用户");
        }
        MyBeanUtils.copyProperties(userVo, user);
        userDao.save(user);
        return ResultUtil.createSuccess("更新用户成功");
    }


    @Override
    public ResponseInfo updateUser(UserVo userVo) {
        ResponseInfo responseInfo = new ResponseInfo();
        User user = userDao.findByToken(userVo.getToken());

        if (user.getCertification()) {
            if (!Objects.equals(user.getSchool(), userVo.getSchool()) ||
                    !Objects.equals(user.getDepartment(), userVo.getDepartment()) ||
                    !Objects.equals(user.getDegree(), userVo.getDegree())) {
                responseInfo.setState("fail");
                responseInfo.setReason("已认证用户不能修改学校信息");
                return responseInfo;
            }
        }

        MyBeanUtils.copyProperties(userVo, user);
        userDao.save(user);
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public ResponseInfo editSchoolInfo(SchoolInfoVo schoolInfoVo) {
        User user = userDao.findByToken(schoolInfoVo.getToken());
        MyBeanUtils.copyProperties(schoolInfoVo, user);
        userDao.save(user);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public ResponseInfo editPersonInfo(UserInfoVo userInfoVo) {
        User user = userDao.findByToken(userInfoVo.getToken());
        MyBeanUtils.copyProperties(userInfoVo, user);
        userDao.save(user);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public ResponseInfo editPreferenceInfo(String token, String hobby, String preference) {
        User user = userDao.findByToken(token);
        user.setHobby(hobby);
        user.setPreference(preference);
        userDao.save(user);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public ResponseInfo editCardSetting(String token, String cardflag) {
        ResponseInfo responseInfo = new ResponseInfo();
        User user = userDao.findByToken(token);

        AvatarVoice avatarVoice = user.getAvatarVoice();

        if (avatarVoice == null) {
            responseInfo.setState("fail");
            responseInfo.setReason("error");
            return responseInfo;
        }

        switch (cardflag) {
            case "0":
                avatarVoice.setCardFlag(false);
                avatarVoiceDao.save(avatarVoice);
                break;
            case "1":
                avatarVoice.setCardFlag(true);
                avatarVoiceDao.save(avatarVoice);
                break;
            default:
                responseInfo.setState("fail");
                responseInfo.setReason("wrong cardflag");
                return responseInfo;
        }

        responseInfo.setState("successful");
        responseInfo.setReason("");
        return responseInfo;
    }

    @Override
    public Map publishActivity(ActivityVo activityVo) {
        User user = userDao.findByToken(activityVo.getToken());
        Activity activity = mapper.map(activityVo, Activity.class);
        activity.setAuthorUser(user);
        activityDao.save(activity);

        Map<String, Object> result = new HashMap<>();
        result.put("id", activity.getId());
        result.put("state", "successful");
        result.put("reason", "");
        return result;
    }

    @Override
    public Map likeActivity(String token, Long activityId) {
        User user = userDao.findByToken(token);
        Activity activity = activityDao.findOne(activityId);

        UserLikeActivityRelation userLikeActivityRelation = new UserLikeActivityRelation(user, activity);

        userLikeActivityRelationDao.save(userLikeActivityRelation);

        int number = activityDao.findOne(activityId).getUserLikeActivityRelations().size();

        Map<String, Object> result = new HashMap<>();
        result.put("state", "successful");
        result.put("reason", "");
        result.put("likenumber", number);


        return result;
    }

    @Override
    public Map unLikeActivity(String token, Long activityId) {
        User user = userDao.findByToken(token);
        Long userId = user.getId();

        javax.persistence.Query query = entityManager.createNativeQuery("DELETE FROM t_user_like_activity_relation WHERE user_id = ?1 and activity_id =?2 ");
        query.setParameter(1, userId);
        query.setParameter(2, activityId);
        query.executeUpdate();

        int number = activityDao.findOne(activityId).getUserLikeActivityRelations().size();
        Map<String, Object> result = new HashMap<>();
        result.put("state", "successful");
        result.put("reason", "");
        result.put("likenumber", number);

        return result;
    }


    @Override
    public ResultInfo deletUserById(Long id) {
        User user = userDao.findOne(id);
        if (user == null) {
            return ResultUtil.createFail("没有此用户");
        }
        userDao.delete(id);
        return ResultUtil.createSuccess("删除用户成功");
    }

    @Override
    public ResultInfo getAllUsers() {
        List<User> users = (List<User>) userDao.findAll();
        List<UserVo> userVos = new ArrayList<>();
        for (User user : users) {
            userVos.add(mapper.map(user, UserVo.class));
        }
        return ResultUtil.createSuccess("所有用户", userVos);
    }


    private boolean checkMsgCode(String phone, String code) {
        CheckMsg checkMsg = checkMsgDao.findByPhone(phone);
        if (checkMsg == null || !checkMsg.getCode().equals(code)) {
            return false;
        }

        if (Minutes.minutesBetween(new DateTime(checkMsg.getTimestamp()), new DateTime()).getMinutes() > 5) {
            return false;
        }
        return true;
    }


    @Override
    public Map getTag(Long userId) {
        User user = userDao.findOne(userId);
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        if (user.getTags() == null) {
            result.put("result", "");
        } else {
            data.put("tags", user.getTags());
            result.put("result", data);
        }

        result.put("state", "successful");
        result.put("reason", "");
        return result;
    }

    @Override
    public Map setTag(String token, String tag) {
        User user = userDao.findByToken(token);
        user.setTags(tag);
        userDao.save(user);

        Map<String, Object> result = new HashMap<>();

        result.put("state", "successful");
        result.put("reason", "");
        return result;
    }

    @Override
    public Map getPersonImages(Long userId, Long imageId) {
        User user = userDao.findOne(userId);
        List<UserImage> userImages = new ArrayList<>();
        Pageable pageable = new PageRequest(0, 10, Sort.Direction.DESC, "id");
        if (imageId == 0L) {
            userImages = userImageDao.getPersonalImages(userId, pageable);
        } else {
            userImages = userImageDao.getPersonalImages2(userId, imageId, pageable);
        }

        List<Map> result = new ArrayList<>();
        for (UserImage userImage : userImages) {
            Map<String, Object> data = new HashMap<>();
            data.put("userid", userId);
            data.put("id", userImage.getId());
            data.put("timestamp", userImage.getTimestamp());
            data.put("username", user.getName());
            data.put("thumbnail", userImage.getThumbnailUrl());
            data.put("image", userImage.getUrl());
            result.add(data);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("state", "successful");
        response.put("reason", "");
        response.put("result", result);
        return response;
    }

    @Override
    public Map getProfile(String token) {
        User user = userDao.findByToken(token);

        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("state", "successful");
        result.put("reason", "");
        result.put("school", user.getSchool());
        result.put("degree", user.getDegree());
        result.put("department", user.getDepartment());
        result.put("enrollment", user.getEnrollment());
        result.put("name", user.getName());
        result.put("gender", user.getGender());
        result.put("birthday", user.getBirthday());
        result.put("preference", user.getPreference());
        result.put("hobby", user.getHobby());
        result.put("phone", user.getPhone());

        result.put("wechat", user.getWechat());
        result.put("qq", user.getQq());
        result.put("hometown", user.getHometown());

        result.put("lookcount", user.getVisitedRelations().size());
        result.put("weme", user.getWeme());
        result.put("id", user.getId());

        return result;
    }

    @Override
    public Map getProfileById(String token, Long userId) {
        User me = userDao.findByToken(token);
        User user = userDao.findOne(userId);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("state", "fail");
            result.put("reason", "用户不存在");
            return result;
        }

        String followFlag = "4";
        if (followRelationDao.findByFollowerAndFollowed(me.getId(), userId) == null) {
            if (followRelationDao.findByFollowerAndFollowed(userId, me.getId()) == null) {
                followFlag = "2";
            } else {
                followFlag = "0";
            }
        } else {
            if (followRelationDao.findByFollowerAndFollowed(userId, me.getId()) == null) {
                followFlag = "1";
            } else {
                followFlag = "3";
            }
        }

        String birthFlag = "0";
        if (me.getBirthday().compareTo(user.getBirthday()) < 0) {
            birthFlag = "1";
        } else if (me.getBirthday().compareTo(user.getBirthday()) > 0) {
            birthFlag = "-1";
        } else {
            birthFlag = "0";
        }


        String voice = "";
        if (user.getAvatarVoice() == null) {
            voice = "";
        } else {
            voice = user.getAvatarVoice().getVoiceUrl();
        }


        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("state", "successful");
        result.put("reason", "");
        result.put("school", user.getSchool());
        result.put("degree", user.getDegree());
        result.put("department", user.getDepartment());
        result.put("enrollment", user.getEnrollment());
        result.put("name", user.getName());
        result.put("gender", user.getGender());
        result.put("birthday", user.getBirthday());
        result.put("preference", user.getPreference());
        result.put("hobby", user.getHobby());
        result.put("phone", user.getPhone());

        result.put("wechat", user.getWechat());
        result.put("qq", user.getQq());
        result.put("hometown", user.getHometown());

        result.put("lookcount", user.getVisitedRelations().size());
        result.put("weme", user.getWeme());
        result.put("id", user.getId());
        result.put("followflag", followFlag);
        result.put("birthflag", birthFlag);
        result.put("certification", user.getCertification());
        result.put("constellation", ConstellationUtils.getConstellation(user.getBirthday()));  //星座
        result.put("voice", voice);

        return result;
    }

    @Override
    public Map getProfileByIdPhone(String token, Long userId) {

        User user = userDao.findOne(userId);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("state", "fail");
            result.put("reason", "用户不存在");
            return result;
        }


        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("state", "successful");
        result.put("reason", "");
        result.put("school", user.getSchool());
        result.put("degree", user.getDegree());
        result.put("department", user.getDepartment());
        result.put("enrollment", user.getEnrollment());
        result.put("name", user.getName());
        result.put("gender", user.getGender());
        result.put("birthday", user.getBirthday());
        result.put("preference", user.getPreference());
        result.put("hobby", user.getHobby());
        result.put("phone", user.getPhone());

        result.put("wechat", user.getWechat());
        result.put("qq", user.getQq());
        result.put("hometown", user.getHometown());

        result.put("lookcount", user.getLookCount());
        result.put("weme", user.getWeme());
        result.put("id", user.getId());

        result.put("certification", user.getCertification());
        result.put("constellation", ConstellationUtils.getConstellation(user.getBirthday()));


        String voice = "";
        if (user.getAvatarVoice() == null) {
            voice = "";
        } else {
            voice = user.getAvatarVoice().getVoiceUrl();
        }
        result.put("voice", voice);

        return result;
    }

    @Override
    public ResponseInfo followUser(String token, Long followedUserId) {
        User follower = userDao.findByToken(token);
        User followed = userDao.findOne(followedUserId);
        FollowRelation followRelation = new FollowRelation(follower, followed);
        followRelationDao.save(followRelation);

        ResponseInfo responseInfo = new ResponseInfo();
        return responseInfo;
    }

}
