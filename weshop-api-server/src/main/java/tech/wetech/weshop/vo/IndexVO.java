package tech.wetech.weshop.vo;

import tech.wetech.weshop.po.*;

import java.util.List;

/**
 * @author cjbi
 */
public class IndexVO {

    private List<Ad> bannerList;

    private List<Brand> brandList;

    private List<HomeCategoryVO> categoryList;

    private List<Channel> channelList;

    private List<Goods> hotGoodsList;

    private List<Goods> newGoodsList;

    private List<Topic> topicList;

    public List<Ad> getBannerList() {
        return bannerList;
    }

    public IndexVO setBannerList(List<Ad> bannerList) {
        this.bannerList = bannerList;
        return this;
    }

    public List<Brand> getBrandList() {
        return brandList;
    }

    public IndexVO setBrandList(List<Brand> brandList) {
        this.brandList = brandList;
        return this;
    }

    public List<HomeCategoryVO> getCategoryList() {
        return categoryList;
    }

    public IndexVO setCategoryList(List<HomeCategoryVO> categoryList) {
        this.categoryList = categoryList;
        return this;
    }

    public List<Channel> getChannelList() {
        return channelList;
    }

    public IndexVO setChannelList(List<Channel> channelList) {
        this.channelList = channelList;
        return this;
    }

    public List<Goods> getHotGoodsList() {
        return hotGoodsList;
    }

    public IndexVO setHotGoodsList(List<Goods> hotGoodsList) {
        this.hotGoodsList = hotGoodsList;
        return this;
    }

    public List<Goods> getNewGoodsList() {
        return newGoodsList;
    }

    public IndexVO setNewGoodsList(List<Goods> newGoodsList) {
        this.newGoodsList = newGoodsList;
        return this;
    }

    public List<Topic> getTopicList() {
        return topicList;
    }

    public IndexVO setTopicList(List<Topic> topicList) {
        this.topicList = topicList;
        return this;
    }
}
