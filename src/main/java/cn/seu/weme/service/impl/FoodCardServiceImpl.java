package cn.seu.weme.service.impl;

import cn.seu.weme.common.result.ResponseInfo;
import cn.seu.weme.common.utils.WEMEGlobalParams;
import cn.seu.weme.controller.old.FoodCardController;
import cn.seu.weme.dao.FoodCardDao;
import cn.seu.weme.dao.LikeFoodCardDao;
import cn.seu.weme.dao.UserDao;
import cn.seu.weme.entity.FoodCard;
import cn.seu.weme.entity.LikeFoodCard;
import cn.seu.weme.entity.User;
import cn.seu.weme.service.FoodCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.CertPathValidatorException;
import java.util.*;

/**
 * Created by Visen (zhvisen@gmail.com) on 2017/1/4.
 */
@Service
@Transactional
public class FoodCardServiceImpl implements FoodCardService {

    public static final Logger logger = LoggerFactory.getLogger(FoodCardController.class);

    @Autowired
    private FoodCardDao foodCardDao;

    @Autowired
    private LikeFoodCardDao likeFoodCardDao;

    @Autowired
    private UserDao userDao;

    @Override
    public ResponseInfo publishCard(String token,String title,String location,String longtitude ,String latitude ,Double price ,String comment)
    {

        User user =userDao.findByToken(token);
        if (user == null) {

            //return result;
            return new ResponseInfo("fail","no user");
        }
        else
        {
            user.setWeme(user.getWeme()+ WEMEGlobalParams.getWEMEPUBLISHFOODCARD());
            userDao.save(user);
            try {
                String strcomment = new String(comment.getBytes(),"UTF-8");
                FoodCard foodcard =new FoodCard();
                foodcard.setTitle(title);
                foodcard.setLocation(location);
                foodcard.setLongitude(longtitude);
                foodcard.setLatitude(latitude);
                foodcard.setPrice(price.toString());
                foodcard.setComment(strcomment);

                foodcard.setAuthor(user);

                foodCardDao.save(foodcard);
                long id = foodcard.getId();

                ResponseInfo responseInfo = new ResponseInfo("successful","");
                responseInfo.setId(id);
                return responseInfo;


            }
            catch (Exception e)
            {
                logger.info(e.getMessage());
                return new ResponseInfo("fail","exception");

            }

        }

    }

    @Override
    public ResponseInfo likeFoodCard(String token,Long foodcardid)
    {
        User user = userDao.findByToken(token);
        Map<String,Object> m =new HashMap<>();
        if (user==null)
        {

           return new ResponseInfo("fail","no user");
        }
        else
        {
            FoodCard foodcard = foodCardDao.findOne(foodcardid);
            LikeFoodCard likeFoodCard=likeFoodCardDao.findOne(foodcard.getId());
            if (likeFoodCard==null)
            {
                likeFoodCard = new LikeFoodCard();
                likeFoodCard.setUser(user);
                likeFoodCard.setFoodCard(foodcard);
                likeFoodCardDao.save(likeFoodCard);
                user.setWeme(user.getWeme()+WEMEGlobalParams.getWEMELIKE());
                userDao.save(user);

                int likeNumber = foodcard.getLikeFoodCards().size();

                ResponseInfo ret = new ResponseInfo("successful","");
                ret.setLikenumber(likeNumber);
                return ret;

            }
            else
            {
                return new ResponseInfo("fail","already like");
            }
        }
    }

    @Override
    public ResponseInfo getFoodCard(String token)
    {
        User user=  userDao.findByToken(token);
        Map<String,Object> m = new HashMap<>();
        if(user==null)
        {
            List<FoodCard> foodCard = foodCardDao.findActivityByPassFlagTrue();
            Random random = new Random();
            List<FoodCard> foodresult = new ArrayList<>();
            for (int i =0;i<Math.min(10,foodCard.size());i++)
            {
               int index= random.nextInt(foodCard.size());
                if(foodresult.contains(foodCard.get(index)))
                {
                    i--;
                    continue;
                }
                else
                    foodresult.add(foodCard.get(index));
            }
            if(foodCard.size()>0)
            {
                ResponseInfo ret = new ResponseInfo("successful","");
                ret.setResult(foodresult);
                return ret;
            }
            else
            {
                return new ResponseInfo("fail","no card");

            }


        }
        else
        {
            return new ResponseInfo("fail","no user");

        }


    }


}
