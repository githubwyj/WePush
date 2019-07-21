package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxAccountMapper;
import com.fangxuele.tool.push.domain.TWxAccount;
import com.fangxuele.tool.push.logic.msgsender.AliDayuTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.AliYunMsgSender;
import com.fangxuele.tool.push.logic.msgsender.HttpMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MailMsgSender;
import com.fangxuele.tool.push.logic.msgsender.TxYunMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMaTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.YunPianMsgSender;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.dialog.MailTestDialog;
import com.fangxuele.tool.push.ui.dialog.SwitchWxAccountDialog;
import com.fangxuele.tool.push.ui.dialog.WxCpAppDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.form.msg.WxCpMsgForm;
import com.fangxuele.tool.push.util.HikariUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * 设置tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/16.
 */
public class SettingListener {
    private static final Log logger = LogFactory.get();

    private static JPanel settingPanel = SettingForm.settingForm.getSettingPanel();

    public static String wxAccountType;

    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    public static void addListeners() {

        // 设置-常规-启动时自动检查更新
        SettingForm.settingForm.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            App.config.setAutoCheckUpdate(SettingForm.settingForm.getAutoCheckUpdateCheckBox().isSelected());
            App.config.save();
        });

        // 设置-公众号-保存
        SettingForm.settingForm.getSettingMpInfoSaveButton().addActionListener(e -> {
            try {
                String accountName;
                if (SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem() == null || StringUtils.isEmpty(SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem().toString())) {
                    accountName = "默认账号";
                } else {
                    accountName = SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem().toString();
                }
                App.config.setWechatMpName(accountName);
                App.config.setWechatAppId(SettingForm.settingForm.getWechatAppIdTextField().getText());
                App.config.setWechatAppSecret(new String(SettingForm.settingForm.getWechatAppSecretPasswordField().getPassword()));
                App.config.setWechatToken(new String(SettingForm.settingForm.getWechatTokenPasswordField().getPassword()));
                App.config.setWechatAesKey(new String(SettingForm.settingForm.getWechatAesKeyPasswordField().getPassword()));

                App.config.setMpUseProxy(SettingForm.settingForm.getMpUseProxyCheckBox().isSelected());
                App.config.setMpProxyHost(SettingForm.settingForm.getMpProxyHostTextField().getText());
                App.config.setMpProxyPort(SettingForm.settingForm.getMpProxyPortTextField().getText());
                App.config.setMpProxyUserName(SettingForm.settingForm.getMpProxyUserNameTextField().getText());
                App.config.setMpProxyPassword(SettingForm.settingForm.getMpProxyPasswordTextField().getText());

                App.config.setMpUseOutSideAt(SettingForm.settingForm.getUseOutSideAccessTokenCheckBox().isSelected());
                App.config.setMpManualAt(SettingForm.settingForm.getManualAtRadioButton().isSelected());
                App.config.setMpApiAt(SettingForm.settingForm.getApiAtRadioButton().isSelected());
                App.config.setMpAt(SettingForm.settingForm.getAccessTokenTextField().getText());
                App.config.setMpAtExpiresIn(SettingForm.settingForm.getAtExpiresInTextField().getText());
                App.config.setMpAtApiUrl(SettingForm.settingForm.getAtApiUrlTextField().getText());

                App.config.save();

                boolean update = false;
                List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MP, accountName);
                if (tWxAccountList.size() > 0) {
                    update = true;
                }

                TWxAccount tWxAccount = new TWxAccount();
                String now = SqliteUtil.nowDateForSqlite();
                tWxAccount.setAccountType(SettingForm.WX_ACCOUNT_TYPE_MP);
                tWxAccount.setAccountName(accountName);
                tWxAccount.setAppId(App.config.getWechatAppId());
                tWxAccount.setAppSecret(App.config.getWechatAppSecret());
                tWxAccount.setToken(App.config.getWechatToken());
                tWxAccount.setAesKey(App.config.getWechatAesKey());
                tWxAccount.setModifiedTime(now);
                if (update) {
                    tWxAccount.setId(tWxAccountList.get(0).getId());
                    wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                } else {
                    tWxAccount.setCreateTime(now);
                    wxAccountMapper.insert(tWxAccount);
                }

                SettingForm.initSwitchMultiAccount();
                WxMpTemplateMsgSender.wxMpConfigStorage = null;
                WxMpTemplateMsgSender.wxMpService = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-公众号-多账号管理
        SettingForm.settingForm.getMpAccountManageButton().addActionListener(e -> {
            SwitchWxAccountDialog dialog = new SwitchWxAccountDialog();
            wxAccountType = SettingForm.WX_ACCOUNT_TYPE_MP;
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
        });

        // 公众号切换账号事件
        SettingForm.settingForm.getMpAccountSwitchComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String accountName = e.getItem().toString();
                List<TWxAccount> wxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MP, accountName);
                if (wxAccountList.size() > 0) {
                    TWxAccount tWxAccount = wxAccountList.get(0);
                    SettingForm.settingForm.getMpAccountSwitchComboBox().setSelectedItem(tWxAccount.getAccountName());
                    SettingForm.settingForm.getWechatAppIdTextField().setText(tWxAccount.getAppId());
                    SettingForm.settingForm.getWechatAppSecretPasswordField().setText(tWxAccount.getAppSecret());
                    SettingForm.settingForm.getWechatTokenPasswordField().setText(tWxAccount.getToken());
                    SettingForm.settingForm.getWechatAesKeyPasswordField().setText(tWxAccount.getAesKey());
                }
            }
        });

        // 设置-小程序-保存
        SettingForm.settingForm.getSettingMaInfoSaveButton().addActionListener(e -> {
            try {
                String accountName;
                if (SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem() == null || StringUtils.isEmpty(SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem().toString())) {
                    accountName = "默认账号";
                } else {
                    accountName = SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem().toString();
                }
                App.config.setMiniAppName(accountName);
                App.config.setMiniAppAppId(SettingForm.settingForm.getMiniAppAppIdTextField().getText());
                App.config.setMiniAppAppSecret(new String(SettingForm.settingForm.getMiniAppAppSecretPasswordField().getPassword()));
                App.config.setMiniAppToken(new String(SettingForm.settingForm.getMiniAppTokenPasswordField().getPassword()));
                App.config.setMiniAppAesKey(new String(SettingForm.settingForm.getMiniAppAesKeyPasswordField().getPassword()));

                App.config.setMaUseProxy(SettingForm.settingForm.getMaUseProxyCheckBox().isSelected());
                App.config.setMaProxyHost(SettingForm.settingForm.getMaProxyHostTextField().getText());
                App.config.setMaProxyPort(SettingForm.settingForm.getMaProxyPortTextField().getText());
                App.config.setMaProxyUserName(SettingForm.settingForm.getMaProxyUserNameTextField().getText());
                App.config.setMaProxyPassword(SettingForm.settingForm.getMaProxyPasswordTextField().getText());
                App.config.save();

                boolean update = false;
                List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MA, accountName);
                if (tWxAccountList.size() > 0) {
                    update = true;
                }

                TWxAccount tWxAccount = new TWxAccount();
                String now = SqliteUtil.nowDateForSqlite();
                tWxAccount.setAccountType(SettingForm.WX_ACCOUNT_TYPE_MA);
                tWxAccount.setAccountName(accountName);
                tWxAccount.setAppId(App.config.getMiniAppAppId());
                tWxAccount.setAppSecret(App.config.getMiniAppAppSecret());
                tWxAccount.setToken(App.config.getMiniAppToken());
                tWxAccount.setAesKey(App.config.getMiniAppAesKey());
                tWxAccount.setModifiedTime(now);
                if (update) {
                    tWxAccount.setId(tWxAccountList.get(0).getId());
                    wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                } else {
                    tWxAccount.setCreateTime(now);
                    wxAccountMapper.insert(tWxAccount);
                }

                SettingForm.initSwitchMultiAccount();
                WxMaTemplateMsgSender.wxMaConfigStorage = null;
                WxMaTemplateMsgSender.wxMaService = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-小程序-多账号管理
        SettingForm.settingForm.getMaAccountManageButton().addActionListener(e -> {
            SwitchWxAccountDialog dialog = new SwitchWxAccountDialog();
            wxAccountType = SettingForm.WX_ACCOUNT_TYPE_MA;
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
        });

        // 小程序切换账号事件
        SettingForm.settingForm.getMaAccountSwitchComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String accountName = e.getItem().toString();
                List<TWxAccount> wxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MA, accountName);
                if (wxAccountList.size() > 0) {
                    TWxAccount tWxAccount = wxAccountList.get(0);
                    SettingForm.settingForm.getMaAccountSwitchComboBox().setSelectedItem(tWxAccount.getAccountName());
                    SettingForm.settingForm.getMiniAppAppIdTextField().setText(tWxAccount.getAppId());
                    SettingForm.settingForm.getMiniAppAppSecretPasswordField().setText(tWxAccount.getAppSecret());
                    SettingForm.settingForm.getMiniAppTokenPasswordField().setText(tWxAccount.getToken());
                    SettingForm.settingForm.getMiniAppAesKeyPasswordField().setText(tWxAccount.getAesKey());
                }
            }
        });

        // 企业号-保存
        SettingForm.settingForm.getWxCpSaveButton().addActionListener(e -> {
            try {
                App.config.setWxCpCorpId(SettingForm.settingForm.getWxCpCorpIdTextField().getText());
                App.config.save();

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-企业号-应用管理
        SettingForm.settingForm.getWxCpAppManageButton().addActionListener(e -> {
            WxCpAppDialog dialog = new WxCpAppDialog();
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
            WxCpMsgForm.initAppNameList();
        });

        // 设置-阿里云短信-保存
        SettingForm.settingForm.getSettingAliyunSaveButton().addActionListener(e -> {
            try {
                App.config.setAliyunAccessKeyId(SettingForm.settingForm.getAliyunAccessKeyIdTextField().getText());
                App.config.setAliyunAccessKeySecret(new String(SettingForm.settingForm.getAliyunAccessKeySecretTextField().getPassword()));
                App.config.setAliyunSign(SettingForm.settingForm.getAliyunSignTextField().getText());
                App.config.save();
                AliYunMsgSender.iAcsClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里大于-保存
        SettingForm.settingForm.getSettingAliInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setAliServerUrl(SettingForm.settingForm.getAliServerUrlTextField().getText());
                App.config.setAliAppKey(new String(SettingForm.settingForm.getAliAppKeyPasswordField().getPassword()));
                App.config.setAliAppSecret(new String(SettingForm.settingForm.getAliAppSecretPasswordField().getPassword()));
                App.config.setAliSign(SettingForm.settingForm.getAliSignTextField().getText());
                App.config.save();
                AliDayuTemplateMsgSender.taobaoClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-腾讯云短信-保存
        SettingForm.settingForm.getSettingTxyunSaveButton().addActionListener(e -> {
            try {
                App.config.setTxyunAppId(SettingForm.settingForm.getTxyunAppIdTextField().getText());
                App.config.setTxyunAppKey(new String(SettingForm.settingForm.getTxyunAppKeyTextField().getPassword()));
                App.config.setTxyunSign(SettingForm.settingForm.getTxyunSignTextField().getText());
                App.config.save();

                TxYunMsgSender.smsSingleSender = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-云片网短信-保存
        SettingForm.settingForm.getSettingYunpianSaveButton().addActionListener(e -> {
            try {
                App.config.setYunpianApiKey(new String(SettingForm.settingForm.getYunpianApiKeyTextField().getPassword()));
                App.config.save();
                YunPianMsgSender.yunpianClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        SettingForm.settingForm.getHttpSaveButton().addActionListener(e -> {
            try {
                App.config.setHttpUseProxy(SettingForm.settingForm.getHttpUseProxyCheckBox().isSelected());
                App.config.setHttpProxyHost(SettingForm.settingForm.getHttpProxyHostTextField().getText());
                App.config.setHttpProxyPort(SettingForm.settingForm.getHttpProxyPortTextField().getText());
                App.config.setHttpProxyUserName(SettingForm.settingForm.getHttpProxyUserTextField().getText());
                App.config.setHttpProxyPassword(SettingForm.settingForm.getHttpProxyPasswordTextField().getText());
                App.config.save();

                HttpMsgSender.proxy = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // E-Mail测试
        SettingForm.settingForm.getTestMailButton().addActionListener(e -> {
            App.config.setMailHost(SettingForm.settingForm.getMailHostTextField().getText());
            App.config.setMailPort(SettingForm.settingForm.getMailPortTextField().getText());
            App.config.setMailFrom(SettingForm.settingForm.getMailFromTextField().getText());
            App.config.setMailUser(SettingForm.settingForm.getMailUserTextField().getText());
            App.config.setMailPassword(new String(SettingForm.settingForm.getMailPasswordField().getPassword()));
            App.config.setMailUseStartTLS(SettingForm.settingForm.getMailStartTLSCheckBox().isSelected());
            App.config.setMailUseSSL(SettingForm.settingForm.getMailSSLCheckBox().isSelected());
            MailMsgSender.mailAccount = null;

            MailTestDialog mailTestDialog = new MailTestDialog();
            mailTestDialog.pack();
            mailTestDialog.setVisible(true);
        });

        // E-Mail保存
        SettingForm.settingForm.getSaveMailButton().addActionListener(e -> {
            try {
                App.config.setMailHost(SettingForm.settingForm.getMailHostTextField().getText());
                App.config.setMailPort(SettingForm.settingForm.getMailPortTextField().getText());
                App.config.setMailFrom(SettingForm.settingForm.getMailFromTextField().getText());
                App.config.setMailUser(SettingForm.settingForm.getMailUserTextField().getText());
                App.config.setMailPassword(new String(SettingForm.settingForm.getMailPasswordField().getPassword()));
                App.config.setMailUseStartTLS(SettingForm.settingForm.getMailStartTLSCheckBox().isSelected());
                App.config.setMailUseSSL(SettingForm.settingForm.getMailSSLCheckBox().isSelected());
                App.config.save();

                MailMsgSender.mailAccount = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        SettingForm.settingForm.getSettingTestDbLinkButton().addActionListener(e -> {
            HikariDataSource hikariDataSource = null;
            try {
                String dbUrl = SettingForm.settingForm.getMysqlUrlTextField().getText();
                String dbUser = SettingForm.settingForm.getMysqlUserTextField().getText();
                String dbPassword = new String(SettingForm.settingForm.getMysqlPasswordField().getPassword());
                if (StringUtils.isBlank(dbUrl)) {
                    SettingForm.settingForm.getMysqlUrlTextField().grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbUser)) {
                    SettingForm.settingForm.getMysqlUserTextField().grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbPassword)) {
                    SettingForm.settingForm.getMysqlPasswordField().grabFocus();
                    return;
                }
                hikariDataSource = new HikariDataSource();
                hikariDataSource.setJdbcUrl("jdbc:mysql://" + dbUrl);
                hikariDataSource.setUsername(dbUser);
                hikariDataSource.setPassword(dbPassword);
                if (hikariDataSource.getConnection() == null) {
                    JOptionPane.showMessageDialog(settingPanel, "连接失败", "失败",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(settingPanel, "连接成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "连接失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                if (hikariDataSource != null) {
                    try {
                        hikariDataSource.close();
                    } catch (Exception e2) {
                        logger.error(e2);
                    }
                }
            }
        });

        // mysql数据库-保存
        SettingForm.settingForm.getSettingDbInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setMysqlUrl(SettingForm.settingForm.getMysqlUrlTextField().getText());
                App.config.setMysqlUser(SettingForm.settingForm.getMysqlUserTextField().getText());
                App.config.setMysqlPassword(new String(SettingForm.settingForm.getMysqlPasswordField().getPassword()));
                App.config.save();

                if (HikariUtil.getHikariDataSource() != null) {
                    HikariUtil.getHikariDataSource().close();
                }

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 外观-保存
        SettingForm.settingForm.getSettingAppearanceSaveButton().addActionListener(e -> {
            try {
                App.config.setTheme(Objects.requireNonNull(SettingForm.settingForm.getSettingThemeComboBox().getSelectedItem()).toString());
                App.config.setFont(Objects.requireNonNull(SettingForm.settingForm.getSettingFontNameComboBox().getSelectedItem()).toString());
                App.config.setFontSize(Integer.parseInt(Objects.requireNonNull(SettingForm.settingForm.getSettingFontSizeComboBox().getSelectedItem()).toString()));
                App.config.save();

                Init.initTheme();
                Init.initGlobalFont();
                SwingUtilities.updateComponentTreeUI(App.mainFrame);
                SwingUtilities.updateComponentTreeUI(MainWindow.mainWindow.getTabbedPane());

                JOptionPane.showMessageDialog(settingPanel, "保存成功！\n\n部分细节将在下次启动时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        SettingForm.settingForm.getMpUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleMpProxyPanel());
        SettingForm.settingForm.getMaUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleMaProxyPanel());
        SettingForm.settingForm.getHttpUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleHttpProxyPanel());
        SettingForm.settingForm.getUseOutSideAccessTokenCheckBox().addChangeListener(e -> SettingForm.toggleMpOutSideAccessTokenPanel());
        SettingForm.settingForm.getManualAtRadioButton().addChangeListener(e -> {
            boolean isSelected = SettingForm.settingForm.getManualAtRadioButton().isSelected();
            if (isSelected) {
                SettingForm.settingForm.getApiAtRadioButton().setSelected(false);
            }
        });
        SettingForm.settingForm.getApiAtRadioButton().addChangeListener(e -> {
            boolean isSelected = SettingForm.settingForm.getApiAtRadioButton().isSelected();
            if (isSelected) {
                SettingForm.settingForm.getManualAtRadioButton().setSelected(false);
            }
        });

        SettingForm.settingForm.getOutSideAtTipsLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么场景下需要使用外部AccessToken？</h1>");
                tipsBuilder.append("<p>调用腾讯公众号接口需要AccessToken，上面配置的AppID、AppSecret等正是为了获得AccessToken；</p>");
                tipsBuilder.append("<p>由于有些企业已经开发了微信公众号相关的服务，不必再次通过上面的AppID等配置再次获取；</p>");
                tipsBuilder.append("<p>而且每次获取都会使之前的失效，加上每个公众号每天获取的次数有限；</p>");
                tipsBuilder.append("<h2>建议每天使用WePush频率很高的时候可以使用此功能</h2>");
                tipsBuilder.append("<h2>反之，可不用设置</h2>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
        SettingForm.settingForm.getManualAtTipsLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>这是什么？</h1>");
                tipsBuilder.append("<h2>手动填写AccessToken和过期时间</h2>");
                tipsBuilder.append("<h2>建议仅在临时使用一次WePush且使用时间不会很长的时候才使用</h2>");
                tipsBuilder.append("<p>请向您所在企业的开发人员索取，注意保密</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
        SettingForm.settingForm.getApiAtTipsLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>这是什么？</h1>");
                tipsBuilder.append("<h2>如果企业已经开发了微信公众号相关的服务，建议使用此项；</h2>");
                tipsBuilder.append("<p>向您所在企业的开发人员索取该接口；</p>");
                tipsBuilder.append("<p>接口使用GET请求，返回格式：</p>");
                tipsBuilder.append("<p>{\"access_token\":\"ACCESS_TOKEN\",\"expires_in\":7200}</p>");
                tipsBuilder.append("<p>请一定注意接口安全性，AccessToken一旦被他人利用，后果不堪设想</p>");
                tipsBuilder.append("<p>例如在接口上添加密钥相关的参数：</p>");
                tipsBuilder.append("<p>示例：http://mydomain.com/wechat/getAccessToken?secret=jad76^j2#SY</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }

}
