import { Options } from 'k6/options';
import { connectionFlow } from '../common';

export let options: Options = {
  scenarios: {
    // smoke: {
    //   executor: 'constant-vus',
    //   vus: 20,
    //   duration: "10m",
    //   gracefulStop: "2m",
    // },
    acapy: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 30 },
        { duration: '30s', target: 30 },
        { duration: '30s', target: 0 },
        // { duration: '1m', target: 150 },
        // { duration: '1m', target: 150 },
        // { duration: '1m', target: 0 },
        // { duration: '1m', target: 200 },
        // { duration: '1m', target: 200 },
        // { duration: '1m', target: 0 },
        // { duration: '1m', target: 250 },
        // { duration: '1m', target: 250 },
        // { duration: '1m', target: 0 },
        // { duration: '1m', target: 300 },
        // { duration: '1m', target: 300 },
        // { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '5m',
    }
  },
  thresholds: {
    http_req_failed: [{
      threshold: 'rate==0',
      // abortOnFail: true,
    }],
    // http_req_duration: ['p(95)<=500'],
    checks: ['rate==1'],
  },
};

export default() => {
  connectionFlow();
}
