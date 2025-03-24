import { expect, test, describe } from 'vitest'
import { api } from '$lib/api';
import * as http from '$lib/http';
import { faker } from '@faker-js/faker';

describe('e2e suite', () => {
  test('create ad with tag', async () => {
    const name = faker.person.fullName();
    await api.register({ name: name, 'email': faker.internet.email(), password: 'aboba' });
    const token = await api.login({ name: name, password: 'aboba' }).then(x => x.jwt.access_token);
    const adId = await api.createAd({ title: 'ad' }, token).then(x => x.id);
    await api.addAdTag(adId, { tag: 'pet' }, token);
    const ad = await api.getAd(adId);
    expect(ad.title).toBe('ad');
    const tags = await api.getAdTags(adId).then(x => x.tags);
    const ads = await api.getAdsWithTag('pet').then(x => x.adIds);
    expect(tags).contain('pet');
    expect(ads).contain(adId);
  });
  test('chats', async () => {
    const name1 = faker.person.fullName();
    const name2 = faker.person.fullName();
    await api.register({ name: name1, 'email': faker.internet.email(), password: 'aboba' }).then(x => x.userId);
    const u2 = await api.register({ name: name2, 'email': faker.internet.email(), password: 'aboba' }).then(x => x.userId);
    const token1 = await api.login({ name: name1, password: 'aboba' }).then(x => x.jwt.access_token);
    const adId = await api.createAd({ title: 'ad' }, token1).then(x => x.id);
    const token2 = await api.login({ name: name2, password: 'aboba' }).then(x => x.jwt.access_token);
    const chatId = await api.createChat(adId, token2).then(x => x.chatId);
    await api.sendMessage(adId, chatId, { text: 'test' }, token2);
    const history = await api.getChatHistory(adId, chatId, token2).then(x => x.messages);
    expect(history.items.length).toBe(1);
    const msg = history.items[0];
    expect(msg?.chatId).toBe(chatId);
    expect(msg?.senderId).toBe(u2);
    expect(msg?.text).toBe('test');
  });
  test("images", async () => {
    const name = faker.person.fullName();
    await api.register({ name: name, 'email': faker.internet.email(), password: 'aboba' });
    const token = await api.login({ name: name, password: 'aboba' }).then(x => x.jwt.access_token);
    const adId = await api.createAd({ title: 'ad' }, token).then(x => x.id);
    const imageId = await api.uploadImage(adId, new File(["test"], "foo.png", { type: "image/png" }), token).then(x => x.id);
    await http.getImage(`ads/${adId}/imgs/${imageId}`);
    await api.deleteImage(adId, imageId, token);
    try {
      await http.getImage(`ads/${adId}/imgs/${imageId}`);
    } catch (e) {
      expect(e != undefined);
    }
  });
});