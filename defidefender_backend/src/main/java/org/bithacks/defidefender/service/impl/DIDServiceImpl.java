package org.bithacks.defidefender.service.impl;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.protocol.base.*;
import com.webank.weid.protocol.request.CptMapArgs;
import com.webank.weid.protocol.request.CreateCredentialPojoArgs;
import com.webank.weid.protocol.request.SetAuthenticationArgs;
import com.webank.weid.protocol.request.SetPublicKeyArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.rpc.*;
import com.webank.weid.service.impl.*;
import org.bithacks.defidefender.service.DIDService;
import org.bithacks.defidefender.utils.CommonUtils;
import org.bithacks.defidefender.utils.ConstantFields;
import org.bithacks.defidefender.utils.PrivateKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DIDServiceImpl implements DIDService {

    private static final Logger logger = LoggerFactory.getLogger(DIDServiceImpl.class);

    private AuthorityIssuerService authorityIssuerService = new AuthorityIssuerServiceImpl();

    private CptService cptService = new CptServiceImpl();

    private CredentialService credentialService = new CredentialServiceImpl();

    private WeIdService weIdService = new WeIdServiceImpl();

    private CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();

    /**
     * set validity period to 360 days by default.
     */
    private static final long EXPIRATION_DATE = 1000L * 60 * 60 * 24 * 360;

    @Override
    public ResponseData<CreateWeIdDataResult> createWeid() {
        logger.info("begin create weId and set attribute");

        // 1, create weId, this method automatically creates public and private keys
        ResponseData<CreateWeIdDataResult> createResult = weIdService.createWeId();
        logger.info(
                "weIdService is result,errorCode:{},errorMessage:{}",
                createResult.getErrorCode(), createResult.getErrorMessage()
        );

        if (createResult.getErrorCode().intValue() != ErrorCode.SUCCESS.getCode()) {
            return createResult;
        }

        // 2, call set public key
        ResponseData<Boolean> setPublicKeyRes = this.setPublicKey(createResult.getResult());
        if (!setPublicKeyRes.getResult()) {
            createResult.setErrorCode(
                    ErrorCode.getTypeByErrorCode(setPublicKeyRes.getErrorCode())
            );
            return createResult;
        }

        // 3, call set authentication
        ResponseData<Boolean> setAuthenticateRes = this.setAuthentication(createResult.getResult());
        if (!setAuthenticateRes.getResult()) {
            createResult.setErrorCode(
                    ErrorCode.getTypeByErrorCode(setAuthenticateRes.getErrorCode())
            );
            return createResult;
        }
        return createResult;
    }

    /**
     * Set Public Key For WeIdentity DID Document.
     *
     * @param createWeIdDataResult the object of CreateWeIdDataResult
     * @return the response data
     */
    private ResponseData<Boolean> setPublicKey(CreateWeIdDataResult createWeIdDataResult) {

        // build setPublicKey parameters.
        SetPublicKeyArgs setPublicKeyArgs = new SetPublicKeyArgs();
        setPublicKeyArgs.setWeId(createWeIdDataResult.getWeId());
        setPublicKeyArgs.setPublicKey(createWeIdDataResult.getUserWeIdPublicKey().getPublicKey());
        setPublicKeyArgs.setType("secp256k1");
        setPublicKeyArgs.setUserWeIdPrivateKey(new WeIdPrivateKey());
        setPublicKeyArgs.getUserWeIdPrivateKey()
                .setPrivateKey(createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());

        // call SDK method to chain set attribute.
        ResponseData<Boolean> setResponse = weIdService.setPublicKey(setPublicKeyArgs);
        return setResponse;
    }

    /**
     * Set Authentication For WeIdentity DID Document.
     *
     * @param createWeIdDataResult createWeIdDataResult the object of CreateWeIdDataResult
     * @return the response data
     */
    private ResponseData<Boolean> setAuthentication(CreateWeIdDataResult createWeIdDataResult) {

        // build setAuthentication parameters.
        SetAuthenticationArgs setAuthenticationArgs = new SetAuthenticationArgs();
        setAuthenticationArgs.setWeId(createWeIdDataResult.getWeId());
        setAuthenticationArgs
                .setPublicKey(createWeIdDataResult.getUserWeIdPublicKey().getPublicKey());
        setAuthenticationArgs.setUserWeIdPrivateKey(new WeIdPrivateKey());
        setAuthenticationArgs.getUserWeIdPrivateKey()
                .setPrivateKey(createWeIdDataResult.getUserWeIdPrivateKey().getPrivateKey());

        // call SDK method to chain set attribute.
        ResponseData<Boolean> setResponse = weIdService.setAuthentication(setAuthenticationArgs);
        logger.info(
                "setAuthentication is result,errorCode:{},errorMessage:{}",
                setResponse.getErrorCode(),
                setResponse.getErrorMessage()
        );
        return setResponse;
    }

    @Override
    public ResponseData<Boolean> registerIssuerType(String issuer, String authorityName) {
        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(issuer);

        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        String privateKey = PrivateKeyUtil.getPrivateKeyByWeId("keys/", issuer);
        System.out.println(privateKey);
        weIdPrivateKey.setPrivateKey(privateKey);
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);

        weIdAuthentication.setWeIdPublicKeyId(issuer);
        AuthorityIssuerService authorityIssuerService = new AuthorityIssuerServiceImpl();
        ResponseData<Boolean> government = authorityIssuerService.registerIssuerType(weIdAuthentication, authorityName);
        return government;
    }

    /**
     * registered CPT.
     *
     * @param publisher  the weId of the publisher
     * @param privateKey the private key of the publisher
     * @param claim      claim is CPT
     * @return returns cptBaseInfo
     */
    @Override
    public ResponseData<CptBaseInfo> registCpt(String publisher, String privateKey, Map<String, Object> claim) {
        // build registerCpt parameters.
        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(publisher);
        weIdAuthentication.setWeIdPrivateKey(new WeIdPrivateKey());
        weIdAuthentication.getWeIdPrivateKey().setPrivateKey(privateKey);

        CptMapArgs cptMapArgs = new CptMapArgs();
        cptMapArgs.setWeIdAuthentication(weIdAuthentication);
        cptMapArgs.setCptJsonSchema(claim);

        // create CPT by SDK
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs);
        logger.info(
                "registerCpt is result,errorCode:{},errorMessage:{}",
                response.getErrorCode(),
                response.getErrorMessage()
        );
        return response;
    }

    /**
     * create credential.
     *
     * @param cptId      the cptId of CPT
     * @param issuer     the weId of issue
     * @param privateKey the private key of issuer
     * @param claimData  the data of claim
     * @return returns credential
     */
    @Override
    public ResponseData<CredentialPojo> createCredential(Integer cptId, String issuer, String privateKey, Map<String, Object> claimData) {
        CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();
        CreateCredentialPojoArgs<Map<String, Object>> createCredentialPojoArgs = new CreateCredentialPojoArgs<Map<String, Object>>();
        createCredentialPojoArgs.setCptId(cptId);
        createCredentialPojoArgs.setIssuer(issuer);
        createCredentialPojoArgs.setExpirationDate(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 100);

        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(issuer);

        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(privateKey);
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);

        weIdAuthentication.setWeIdPublicKeyId(issuer + "#key0");
        createCredentialPojoArgs.setWeIdAuthentication(weIdAuthentication);

        createCredentialPojoArgs.setClaim(claimData);
        // 生成凭证
        ResponseData<CredentialPojo> response = credentialPojoService.createCredential(createCredentialPojoArgs);
        // 保存凭证
        CommonUtils.writeObjectToFile(response.getResult(), claimData.get("weid").toString(), 0);
        return response;
    }

    @Override
    public ResponseData<CredentialPojo> createSelectiveCredential(String weid, String claimPolicyJson) {
        CredentialPojo credentialPojo = CommonUtils.readObjectFromFile(weid, 0);
        CredentialPojoService credentialPojoService = new CredentialPojoServiceImpl();

        // 选择性披露
        ClaimPolicy claimPolicy = new ClaimPolicy();
        claimPolicy.setFieldsToBeDisclosed(claimPolicyJson);
        ResponseData<CredentialPojo> selectiveResponse =
                credentialPojoService.createSelectiveCredential(credentialPojo, claimPolicy);
        CommonUtils.writeObjectToFile(selectiveResponse.getResult(), weid, 1);
        return selectiveResponse;
    }

    @Override
    public ResponseData<PresentationE> createPresentation(String ownerWeid, String policyJson) {
        CredentialPojo credentialPojo = CommonUtils.readObjectFromFile(ownerWeid, 0);
        List<CredentialPojo> credentialList = new ArrayList<CredentialPojo>();
        credentialList.add(credentialPojo);
        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        weIdAuthentication.setWeId(ownerWeid);
        WeIdPrivateKey weIdPrivateKey = new WeIdPrivateKey();
        weIdPrivateKey.setPrivateKey(PrivateKeyUtil.getPrivateKeyByWeId(ConstantFields.KEY_DIR, ownerWeid));
        weIdAuthentication.setWeIdPrivateKey(weIdPrivateKey);
        weIdAuthentication.setWeIdPublicKeyId(ownerWeid + "#key0");
        // 创建challenge
        Challenge challenge = Challenge.create(ownerWeid, "Company Presentation");
        challenge.setNonce("Verify Authenticity");
        // 创建presentationPolicyE
        PresentationPolicyE presentationPolicyE = PresentationPolicyE.fromJson(policyJson);
        //创建Presentation
        ResponseData<PresentationE> presentationE = credentialPojoService.createPresentation(credentialList, presentationPolicyE, challenge, weIdAuthentication);
        CommonUtils.writePresentationToFile(presentationE.getResult(), ownerWeid);
        return presentationE;
    }

    @Override
    public ResponseData<Boolean> verifyPresentation(String weid, String policyJson) {
        PresentationE presentation = CommonUtils.readPresentationFromFile(weid);
        // 创建challenge
        Challenge challenge = Challenge.create(weid, "Company Presentation");
        challenge.setNonce("Verify Authenticity");
        // 创建presentationPolicyE
        PresentationPolicyE presentationPolicyE = PresentationPolicyE.fromJson(policyJson);
        ResponseData<Boolean> verifyResult = credentialPojoService.verify(weid, presentationPolicyE, challenge, presentation);
        return verifyResult;
    }

    @Override
    public ResponseData<Boolean> verifyCredential(String weid, String issuer, int type) {
        CredentialPojo credentialPojo = CommonUtils.readObjectFromFile(weid, type);
        ResponseData<Boolean> verifyResult = credentialPojoService.verify(issuer, credentialPojo);
        return verifyResult;
    }
}
