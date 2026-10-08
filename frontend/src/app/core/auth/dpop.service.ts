import { Service } from '@angular/core';

const DATABASE_NAME = 'hub-dpop';
const STORE_NAME = 'keys';
const KEY_ID = 'session';

@Service()
export class DpopService {
  private keyPair: Promise<CryptoKeyPair> | null = null;

  isSupported(): boolean {
    return typeof crypto !== 'undefined' && !!crypto.subtle && typeof indexedDB !== 'undefined';
  }

  async createProof(method: string, url: string, accessToken?: string): Promise<string | null> {
    if (!this.isSupported()) {
      return null;
    }

    const { privateKey, publicKey } = await this.getKeyPair();
    const jwk = await crypto.subtle.exportKey('jwk', publicKey);

    const header = {
      typ: 'dpop+jwt',
      alg: 'ES256',
      jwk: { kty: jwk.kty, crv: jwk.crv, x: jwk.x, y: jwk.y },
    };
    const payload: Record<string, unknown> = {
      jti: crypto.randomUUID(),
      htm: method.toUpperCase(),
      htu: this.targetOf(url),
      iat: Math.floor(Date.now() / 1000),
    };
    if (accessToken) {
      payload['ath'] = await this.hashOf(accessToken);
    }

    const signingInput = `${this.encodeJson(header)}.${this.encodeJson(payload)}`;
    const signature = await crypto.subtle.sign(
      { name: 'ECDSA', hash: 'SHA-256' },
      privateKey,
      new TextEncoder().encode(signingInput),
    );

    return `${signingInput}.${this.toBase64Url(new Uint8Array(signature))}`;
  }

  async clear(): Promise<void> {
    this.keyPair = null;
    if (!this.isSupported()) {
      return;
    }
    const database = await this.openDatabase();
    await this.request(database.transaction(STORE_NAME, 'readwrite').objectStore(STORE_NAME).delete(KEY_ID));
  }

  private getKeyPair(): Promise<CryptoKeyPair> {
    this.keyPair ??= this.loadOrCreateKeyPair();
    return this.keyPair;
  }

  private async loadOrCreateKeyPair(): Promise<CryptoKeyPair> {
    const database = await this.openDatabase();
    const stored = await this.request<CryptoKeyPair | undefined>(
      database.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).get(KEY_ID),
    );
    if (stored) {
      return stored;
    }

    const created = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, false, [
      'sign',
      'verify',
    ]);
    await this.request(
      database.transaction(STORE_NAME, 'readwrite').objectStore(STORE_NAME).put(created, KEY_ID),
    );
    return created;
  }

  private openDatabase(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DATABASE_NAME, 1);
      request.onupgradeneeded = () => request.result.createObjectStore(STORE_NAME);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  private request<T>(request: IDBRequest<T>): Promise<T> {
    return new Promise((resolve, reject) => {
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  private targetOf(url: string): string {
    const parsed = new URL(url);
    return parsed.origin + parsed.pathname;
  }

  private async hashOf(text: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
    return this.toBase64Url(new Uint8Array(digest));
  }

  private encodeJson(value: unknown): string {
    return this.toBase64Url(new TextEncoder().encode(JSON.stringify(value)));
  }

  private toBase64Url(bytes: Uint8Array): string {
    let binary = '';
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
  }
}
