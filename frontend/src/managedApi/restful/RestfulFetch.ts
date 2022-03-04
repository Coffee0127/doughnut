import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";
import loginOrRegister from "./loginOrRegister";

function objectToFormData(data: any) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    if (data[key] === null) {
      formData.append(key, '');
    } else if (data[key] instanceof Object && !(data[key] instanceof File)) {
      Object.keys(data[key]).forEach((subKey) => {
        formData.append(
          `${key}.${subKey}`,
          data[key][subKey] === null ? '' : data[key][subKey]
        );
      });
    } else {
      formData.append(key, data[key]);
    }
  });
  return formData;
}

const request = async (url: string, data: any, {method="GET", contentType='json'}) => {
  const headers = new Headers();
  headers.set('Accept', 'application/json');
  let body: any;
  if (method !== "GET") {
    if (contentType === 'json') {
      headers.set("Content-Type", 'application/json');
      body = JSON.stringify(data)
    }
    else {
      body = objectToFormData(data)
    }
  }
  const res = await fetch(url, {method, headers, body})
  if (res.status === 200 || res.status === 400) {
    return res;
  }
  if (res.status === 204) {
    return {status: 204, json: ()=>null, text: ()=>null};
  }
  if (res.status === 401) {
    loginOrRegister();
  }
  throw new HttpResponseError(res.status);
}

class RestfulFetch {
  base_url: string

  constructor(base_url: string) {
    this.base_url = base_url
  }

  private expandUrl(url: string): string {
    if(url.startsWith("/")) return url;
    return this.base_url + url;
  }

  async restRequest(url: string, data: any, params: any) {
    const response = await request(this.expandUrl(url), data, params);
    const jsonResponse = await response.json()
    if (response.status === 400) throw new BadRequestError(jsonResponse.errors);
    return jsonResponse;
  }

  async restRequestWithHtmlResponse(url: string, data: any, params: any) {
    const response = await request(this.expandUrl(url), data, params)
    if (response.status === 400) throw Error("BadRequest");
    return response.text();
  }
}

export default RestfulFetch;