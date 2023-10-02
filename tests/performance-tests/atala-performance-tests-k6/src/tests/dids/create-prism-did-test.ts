import { Options } from 'k6/options';
import { Issuer } from '../../actors';
import { defaultOptions } from "../../scenarios/default";

export let options: Options = defaultOptions

const issuer = new Issuer();

export default () => {
  group("Issuer create unpublished DID", function () {
    issuer.createUnpublishedDid();
  });
};
