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
};

export type User = {
  id: string;
  name: string;
  email: string;
};
