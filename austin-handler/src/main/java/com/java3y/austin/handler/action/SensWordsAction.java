package com.java3y.austin.handler.action;

import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.dto.model.*;
import com.java3y.austin.common.enums.ChannelType;
import com.java3y.austin.common.enums.EnumUtil;
import com.java3y.austin.common.pipeline.BusinessProcess;
import com.java3y.austin.common.pipeline.ProcessContext;
import com.java3y.austin.handler.config.SensitiveWordsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * 敏感词过滤
 *
 * @author xiaoxiamao
 * @date 2024/08/17
 */
@Service
public class SensWordsAction implements BusinessProcess<TaskInfo> {


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 过滤逻辑
     *
     * @param context
     *
     * @see com.java3y.austin.common.enums.ChannelType
     */
    @Override
    public void process(ProcessContext<TaskInfo> context) {
        // 获取敏感词典
        Set<String> sensDict = Optional.ofNullable(redisTemplate.opsForSet().members(SensitiveWordsConfig.SENS_WORDS_DICT))
                .orElse(Collections.emptySet());
        // 如果敏感词典为空，不过滤
        if (ObjectUtils.isEmpty(sensDict)) {
            return;
        }
        ChannelType channelType = EnumUtil.getEnumByCode(context.getProcessModel().getSendChannel(), ChannelType.class);
        if (Objects.isNull(channelType)) {
            return;
        }
        switch (channelType) {
            case IM:
                // 无文本内容，暂不做过滤处理
                break;
            case PUSH:
                PushContentModel pushContentModel =
                        (PushContentModel) context.getProcessModel().getContentModel();
                pushContentModel.setContent(filter(pushContentModel.getContent(), sensDict));
                break;
            case SMS:
                SmsContentModel smsContentModel =
                        (SmsContentModel) context.getProcessModel().getContentModel();
                smsContentModel.setContent(filter(smsContentModel.getContent(), sensDict));
                break;
            case EMAIL:
                EmailContentModel emailContentModel =
                        (EmailContentModel) context.getProcessModel().getContentModel();
                emailContentModel.setContent(filter(emailContentModel.getContent(), sensDict));
                break;
            case OFFICIAL_ACCOUNT:
                // 无文本内容，暂不做过滤处理
                break;
            case MINI_PROGRAM:
                // 无文本内容，暂不做过滤处理
                break;
            case ENTERPRISE_WE_CHAT:
                EnterpriseWeChatContentModel enterpriseWeChatContentModel =
                        (EnterpriseWeChatContentModel) context.getProcessModel().getContentModel();
                enterpriseWeChatContentModel.setContent(filter(enterpriseWeChatContentModel.getContent(), sensDict));
                break;
            case DING_DING_ROBOT:
                DingDingRobotContentModel dingDingRobotContentModel =
                        (DingDingRobotContentModel) context.getProcessModel().getContentModel();
                dingDingRobotContentModel.setContent(filter(dingDingRobotContentModel.getContent(), sensDict));
                break;
            case DING_DING_WORK_NOTICE:
                DingDingWorkContentModel dingDingWorkContentModel =
                        (DingDingWorkContentModel) context.getProcessModel().getContentModel();
                dingDingWorkContentModel.setContent(filter(dingDingWorkContentModel.getContent(), sensDict));
                break;
            case ENTERPRISE_WE_CHAT_ROBOT:
                EnterpriseWeChatRobotContentModel enterpriseWeChatRobotContentModel =
                        (EnterpriseWeChatRobotContentModel) context.getProcessModel().getContentModel();
                enterpriseWeChatRobotContentModel.setContent(filter(enterpriseWeChatRobotContentModel.getContent(), sensDict));
                break;
            case FEI_SHU_ROBOT:
                FeiShuRobotContentModel feiShuRobotContentModel =
                        (FeiShuRobotContentModel) context.getProcessModel().getContentModel();
                feiShuRobotContentModel.setContent(filter(feiShuRobotContentModel.getContent(), sensDict));
                break;
            case ALIPAY_MINI_PROGRAM:
                // 无文本内容，暂不做过滤处理
                break;
            default:
                break;
        }
    }

    /**
     * 敏感词替换成对应长度'*'
     *
     * @param content
     * @param sensDict
     * @return
     */
    private String filter(String content, Set<String> sensDict) {
        if (ObjectUtils.isEmpty(content) || ObjectUtils.isEmpty(sensDict)) {
            return content;
        }
        // 构建字典树
        TrieNode root = buildTrie(sensDict);
        StringBuilder result = new StringBuilder();
        int n = content.length();
        int i = 0;

        while (i < n) {
            TrieNode node = root;
            int j = i;
            int lastMatchEnd = -1;

            while (j < n && node != null) {
                node = node.children.get(content.charAt(j));
                if (node != null && node.isEnd) {
                    lastMatchEnd = j;
                }
                j++;
            }

            if (lastMatchEnd != -1) {
                // 找到敏感词，用'*'替换
                for (int k = i; k <= lastMatchEnd; k++) {
                    result.append('*');
                }
                i = lastMatchEnd + 1;
            } else {
                result.append(content.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 构建字典树
     *
     * @param sensDict
     * @return
     */
    private TrieNode buildTrie(Set<String> sensDict) {
        TrieNode root = new TrieNode();
        for (String word : sensDict) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new TrieNode());
            }
            node.isEnd = true;
        }
        return root;
    }

    /**
     * 树节点
     */
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        // 是否为叶子节点
        boolean isEnd = false;
    }

}
