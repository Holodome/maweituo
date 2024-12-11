import * as http from "$lib/http";

export type Token = string & { readonly Token: unique symbol };
export type UserId = string & { readonly UserId: unique symbol };
export type AdId = string & { readonly AdId: unique symbol };
export type ChatId = string & { readonly ChatId: unique symbol };
export type ImageId = string & { readonly ImageId: unique symbol };

export type Pagination = {
  page: number;
  pageSize: number;
};

export type PaginatedCollection<T> = {
  items: T[];
  pag: Pagination;
  totalPages: number;
  totalItems: number;
};

export type FeedResponseDTO = {
  feed: PaginatedCollection<AdId>;
};

export type LoginRequestDto = {
  name: string;
  password: string;
};

export type LoginResponseDto = {
  jwt: { access_token: Token }
};

export type RegisterRequestDto = {
  name: string;
  email: string;
  password: string;
};

export type RegisterResponseDto = {
  userId: UserId;
};

export type UpdateUserRequestDto = {
  name?: string;
  email?: string;
  password?: string;
};

export type UserPublicInfoDto = {
  id: UserId;
  name: string;
  email: string;
};

export type UserAdsResponseDto = {
  userId: UserId;
  ads: AdId[];
};

export type UserChatsResponseDto = {
  userId: UserId;
  chats: ChatId[];
};

export type AdTagsResponseDto = {
  adId: AdId;
  tags: string[];
};

export type DeleteTagRequestDto = {
  tag: string;
};

export type AdResponseDto = {
  id: AdId;
  authorId: UserId;
  title: string;
  resolved: boolean;
  createdAt: Date;
  updatedAt: Date;
};

export type CreateAdRequestDto = {
  title: string;
};

export type UpdateAdRequestDto = {
  resolved?: boolean;
  title?: string;
};

export type MarkAdResolvedRequestDto = {
  withWhom: UserId;
};

export type MessageDto = {
  senderId: UserId;
  chatId: ChatId;
  text: string;
  at: Date;
};

export type HistoryResponseDto = {
  chatId: ChatId;
  messages: PaginatedCollection<MessageDto>;
};

export type SendMessageRequestDto = {
  text: string;
};

export type ChatDto = {
  id: ChatId;
  adId: AdId;
  adAuthor: UserId;
  clientId: UserId;
};

export type CreateChatResponseDto = {
  chatId: ChatId;
};

export type AdChatsResponseDto = {
  adId: AdId;
  chats: ChatDto[];
};

export type CreateImageRequestDto = {
  id: ImageId;
};

export type AllTagsResponse = {
  tags: string[];
};

export type TagAdsResponse = {
  tag: string;
  adIds: AdId[];
};

export type AddTagRequestDto = {
  tag: string;
};

export type CreateImageResponseDto = {
  id: ImageId;
};

export type AdImagesResponseDto = {
  adId: AdId;
  images: ImageId[];
};

export type CreateAdResponseDto = {
  id: AdId;
};

export interface AppApi {
  login(dto: LoginRequestDto): Promise<LoginResponseDto>;
  register(dto: RegisterRequestDto): Promise<RegisterResponseDto>;
  logout(auth: Token): Promise<null>;

  getUser(userId: UserId): Promise<UserPublicInfoDto>;
  deleteUser(userId: UserId, auth: Token): Promise<null>;
  updateUser(userId: UserId, dto: UpdateUserRequestDto, auth: Token): Promise<null>;
  getUserAds(userId: UserId): Promise<UserAdsResponseDto>;

  getAllTags(): Promise<AllTagsResponse>;
  getAdsWithTag(tag: string): Promise<TagAdsResponse>;

  createAd(dto: CreateAdRequestDto, auth: Token): Promise<CreateAdResponseDto>;
  getAd(adId: AdId): Promise<AdResponseDto>;
  updateAd(adId: AdId, dto: UpdateAdRequestDto, auth: Token): Promise<null>;
  deleteAd(adId: AdId, auth: Token): Promise<null>;

  uploadImage(adId: AdId, file: File, auth: Token): Promise<CreateImageResponseDto>;
  getAdImages(adId: AdId): Promise<AdImagesResponseDto>;
  deleteImage(adId: AdId, imageId: ImageId, auth: Token): Promise<null>;

  getAdTags(adId: AdId): Promise<AdTagsResponseDto>;
  addAdTag(adId: AdId, dto: AddTagRequestDto, auth: Token): Promise<null>;
  deleteAdTag(adId: AdId, dto: AddTagRequestDto, auth: Token): Promise<null>;

  getChatHistory(adId: AdId, chatId: ChatId, auth: Token): Promise<HistoryResponseDto>;
  sendMessage(adId: AdId, chatId: ChatId, dto: SendMessageRequestDto, auth: Token): Promise<null>;

  getChat(adId: AdId, chatId: ChatId, auth: Token): Promise<ChatDto>;
  createChat(adId: AdId, auth: Token): Promise<CreateChatResponseDto>;
};

export class AppApiImplementation implements AppApi {
  async login(dto: LoginRequestDto): Promise<LoginResponseDto> {
    return http.post('login', dto).then(x => x as LoginResponseDto);
  }
  async register(dto: RegisterRequestDto): Promise<RegisterResponseDto> {
    return http.post('register', dto).then(x => x as RegisterResponseDto);
  }
  async logout(auth: Token): Promise<null> {
    return http.post('logout', {}, auth).then(() => null);
  }

  async getUser(userId: UserId): Promise<UserPublicInfoDto> {
    return http.get(`users/${userId}`).then(x => x as UserPublicInfoDto);
  }
  async deleteUser(userId: UserId, auth: Token): Promise<null> {
    return http.del(`users/${userId}`, {}, auth).then(() => null);
  }
  async updateUser(userId: UserId, dto: UpdateUserRequestDto, auth: Token): Promise<null> {
    return http.put(`users/${userId}`, dto, auth).then(() => null);
  }
  async getUserAds(userId: UserId): Promise<UserAdsResponseDto> {
    return http.get(`users/${userId}/ads`).then(x => x as UserAdsResponseDto);
  }

  async getAllTags(): Promise<AllTagsResponse> {
    return http.get(`tags`).then(x => x as AllTagsResponse);
  }
  async getAdsWithTag(tag: string): Promise<TagAdsResponse> {
    return http.get(`tags/${tag}/ads`).then(x => x as TagAdsResponse);
  }

  async createAd(dto: CreateAdRequestDto, auth: Token): Promise<CreateAdResponseDto> {
    return http.post(`ads`, dto, auth).then(x => x as CreateAdResponseDto);
  }
  async getAd(adId: AdId): Promise<AdResponseDto> {
    return http.get(`ads/${adId}`).then(x => x as AdResponseDto);
  }
  async updateAd(adId: AdId, dto: UpdateAdRequestDto, auth: Token): Promise<null> {
    return http.put(`ads/${adId}`, dto, auth).then(() => null);
  }
  async deleteAd(adId: AdId, auth: Token): Promise<null> {
    return http.del(`ads/${adId}`, {}, auth).then(() => null);
  }

  async uploadImage(adId: AdId, file: File, auth: Token): Promise<CreateImageResponseDto> {
    return http.postFile(`ads/${adId}/imgs`, file, auth)
      .then(async (x) => await x.text())
      .then(x => JSON.parse(x) as CreateImageResponseDto);
  }
  async getAdImages(adId: AdId): Promise<AdImagesResponseDto> {
    return http.get(`ads/${adId}/imgs`).then(x => x as AdImagesResponseDto);
  }
  async deleteImage(adId: AdId, imageId: ImageId, auth: Token): Promise<null> {
    return http.del(`ads/${adId}/imgs/${imageId}`, {}, auth).then(() => null);
  }

  async getAdTags(adId: AdId): Promise<AdTagsResponseDto> {
    return http.get(`ads/${adId}/tags`).then(x => x as AdTagsResponseDto);
  }
  async addAdTag(adId: AdId, dto: AddTagRequestDto, auth: Token): Promise<null> {
    return http.post(`ads/${adId}/tags`, dto, auth).then(() => null);
  }
  async deleteAdTag(adId: AdId, dto: AddTagRequestDto, auth: Token): Promise<null> {
    return http.del(`ads/${adId}/tags`, dto, auth).then(() => null);
  }

  async getChatHistory(adId: AdId, chatId: ChatId, auth: Token, page?: number): Promise<HistoryResponseDto> {
    const pagination = page !== undefined ? `?page=${page}` : "?page=0";
    return http.get(`ads/${adId}/chats/${chatId}/msgs` + pagination, auth).then(x => x as HistoryResponseDto);
  }
  async sendMessage(adId: AdId, chatId: ChatId, dto: SendMessageRequestDto, auth: Token): Promise<null> {
    return http.post(`ads/${adId}/chats/${chatId}/msgs`, dto, auth).then(() => null);
  }

  async getChat(adId: AdId, chatId: ChatId, auth: Token): Promise<ChatDto> {
    return http.get(`ads/${adId}/chats/${chatId}`, auth).then(x => x as ChatDto);
  }
  async createChat(adId: AdId, auth: Token): Promise<CreateChatResponseDto> {
    return http.post(`ads/${adId}/chats`, {}, auth).then(x => x as CreateChatResponseDto);
  }
};

const api = new AppApiImplementation();
export { api };
