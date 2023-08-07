import { group } from 'k6';
import { Options } from 'k6/options';
import {issuer, holder} from '../common';
import { Connection, CredentialSchemaResponse } from '@input-output-hk/prism-typescript-client';

export let options: Options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 10,
      duration: "1m",
    },
  },
  thresholds: {
    http_req_failed: [{
      threshold: 'rate==0',
      abortOnFail: true,
    }],
    http_req_duration: ['p(95)<=500'],
    checks: ['rate==1'],
  },
};

// This is setup code. It runs once at the beginning of the test, regardless of the number of VUs.
export function setup() {

  group('Issuer publishes DID', function () {
    issuer.createUnpublishedDid();
    issuer.publishDid();
  });

  group('Issuer creates credential schema', function () {
    issuer.createCredentialSchema();
  });

  group('Holder creates unpublished DID', function () {
    holder.createUnpublishedDid();
  });

  group('Issuer connects with Holder', function () {
    issuer.createHolderConnection();
    holder.acceptIssuerConnection(issuer.connectionWithHolder!.invitation);
    issuer.finalizeConnectionWithHolder();
    holder.finalizeConnectionWithIssuer();
  });

  return { issuerDid: issuer.did, holderDid: holder.did, issuerSchema: issuer.schema, connectionWithHolder: issuer.connectionWithHolder, connectionWithIssuer: holder.connectionWithIssuer };
}

export default (data: { issuerDid: string; holderDid: string; issuerSchema: CredentialSchemaResponse, connectionWithHolder: Connection, connectionWithIssuer: Connection}) => {

  // This is the only way to pass data from setup to default
  issuer.did = data.issuerDid;
  issuer.schema = data.issuerSchema;
  issuer.connectionWithHolder = data.connectionWithHolder;
  holder.did = data.holderDid;
  holder.connectionWithIssuer = data.connectionWithIssuer;

  group('Issuer creates credential offer for Holder', function () {
    issuer.createCredentialOffer();
    issuer.waitForCredentialOfferToBeSent();
  });
  
  group('Holder achieves and accepts credential offer from Issuer', function () {
    holder.waitAndAcceptCredentialOffer(issuer.credential!.thid);
  });

  group('Issuer issues credential to Holder', function () {
    issuer.receiveCredentialRequest();
    issuer.issueCredential();
    issuer.waitForCredentialToBeSent();
  });

  group('Holder receives credential from Issuer', function () {
    holder.receiveCredential();
  });

};
