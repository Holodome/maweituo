export type Token = string & { readonly Token: unique symbol };
export type UserId = string & { readonly UserId: unique symbol };

export type Feed = {
  ads: string[];
  total: number;
};

export type Advertisement = {
  id: string;
  title: string;
  authorId: string;
  tags: string[];
  images: string[];
  chats: string[];
};

export type User = {
  id: string;
  name: string;
  email: string;
};

export type Message = {
  sender: string;
  text: string;
  chat: string;
  at: string;
};

export type Chat = {
  id: string,
  adId: string,
  adAuthor: string,
  client: string
};
