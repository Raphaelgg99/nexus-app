import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import {
  Edit3,
  Folder,
  LayoutDashboard,
  LucideAngularModule,
  LogOut,
  Power,
  Plus,
  ShieldUser,
  Trash2,
  Upload,
  UserPlus,
  WandSparkles,
  X,
} from 'lucide-angular';
import { finalize, forkJoin, of, switchMap } from 'rxjs';

import { Chatbot } from '../../services/chatbot';
import { Auth } from '../../services/auth';
import {
  FolderMockup,
  FolderResponse,
  MockupRequest,
  MockupResponse,
} from '../../services/folder-mockup';

type PendingProductDraft = {
  file: File;
  previewUrl: string;
  name: string;
  description: string;
  sku: string;
  available: boolean;
  stockQuantity: number;
};

type ProductModalMode = 'create' | 'manage';

type ManagedProductDraft = {
  id: number;
  image: string;
  name: string;
  description: string;
  sku: string;
  available: boolean;
  stockQuantity: number;
  isDeleting: boolean;
};

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class Sidebar {
  private static readonly elevenLabsScriptId = 'elevenlabs-convai-widget-script';
  private static readonly elevenLabsScriptSrc =
    'https://unpkg.com/@elevenlabs/convai-widget-embed';
  private static readonly elevenLabsAgentId = 'agent_2701kn38jvn8fta9tmjr33q3sa3w';

  readonly iconDashboard = LayoutDashboard;
  readonly iconImageGenerator = WandSparkles;
  readonly iconFolders = Folder;
  readonly iconLeads = UserPlus;
  readonly iconAdmin = ShieldUser;
  readonly iconPower = Power;
  readonly iconLogout = LogOut;
  readonly iconAdd = Plus;
  readonly iconEdit = Edit3;
  readonly iconRemove = Trash2;
  readonly iconUpload = Upload;
  readonly iconClose = X;

  folders: FolderResponse[] = [];
  isLoadingFolders = false;
  folderActionError = '';

  isChatBotActive = false;
  isLoadingChatBot = false;
  isTogglingChatBot = false;
  isFoldersMenuOpen = false;
  selectedFolderIndex = 0;

  isMockupModalOpen = false;
  isElevenLabsModalOpen = false;
  productModalMode: ProductModalMode = 'create';
  activeFolderId: number | null = null;
  activeFolderName = '';
  pendingProducts: PendingProductDraft[] = [];
  managedProducts: ManagedProductDraft[] = [];
  isLoadingManagedProducts = false;
  isSavingProducts = false;
  isSavingManagedProducts = false;
  mockupModalError = '';
  mockupModalSuccess = '';

  chatBotStatusLabel = 'Carregando';
  chatBotError = '';

  constructor(
    private readonly auth: Auth,
    private readonly router: Router,
    private readonly chatbot: Chatbot,
    private readonly folderMockup: FolderMockup,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {
    this.loadFolders();
    this.loadChatBotStatus();
  }

  get selectedFolder(): FolderResponse | null {
    return this.folders[this.selectedFolderIndex] ?? null;
  }

  get currentUserName(): string {
    return this.auth.getCurrentUser()?.userName ?? 'Usuario';
  }

  get isAdmin(): boolean {
    return this.auth.userIsAdmin();
  }

  get elevenLabsAgentId(): string {
    return Sidebar.elevenLabsAgentId;
  }

  get isManageMode(): boolean {
    return this.productModalMode === 'manage';
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }

  openElevenLabsModal(): void {
    this.isElevenLabsModalOpen = true;
    this.ensureElevenLabsWidgetScript();
  }

  closeElevenLabsModal(): void {
    this.isElevenLabsModalOpen = false;
  }

  loadChatBotStatus(): void {
    this.isLoadingChatBot = true;
    this.chatBotError = '';

    this.chatbot
      .getStatus()
      .pipe(finalize(() => (this.isLoadingChatBot = false)))
      .subscribe({
        next: (chatbot) => {
          this.updateChatBotStatus(chatbot.active);
        },
        error: () => {
          this.isLoadingChatBot = false;
          this.chatBotStatusLabel = 'Indisponivel';
          this.chatBotError = 'Nao foi possivel carregar o status.';
          this.changeDetectorRef.detectChanges();
        },
      });
  }

  toggleChatBotStatus(): void {
    if (this.isTogglingChatBot) {
      return;
    }

    this.isTogglingChatBot = true;
    this.chatBotError = '';

    this.chatbot
      .toggleStatus()
      .pipe(
        finalize(() => {
          this.isTogglingChatBot = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: (chatbot) => {
          this.updateChatBotStatus(chatbot.active);
        },
        error: () => {
          this.chatBotError = 'Nao foi possivel alterar o status.';
          this.changeDetectorRef.detectChanges();
        },
      });
  }

  toggleFoldersMenu(): void {
    this.isFoldersMenuOpen = !this.isFoldersMenuOpen;
  }

  selectFolder(index: number): void {
    if (!this.folders[index]) {
      return;
    }

    this.selectedFolderIndex = index;
  }

  openCreateProductModal(index: number): void {
    const folder = this.prepareFolderContext(index);

    if (!folder) {
      return;
    }

    this.productModalMode = 'create';
    this.clearPendingProducts();
    this.clearManagedProducts();
    this.mockupModalError = '';
    this.mockupModalSuccess = '';
    this.isMockupModalOpen = true;
  }

  openManageProductsModal(index: number): void {
    const folder = this.prepareFolderContext(index);

    if (!folder) {
      return;
    }

    this.productModalMode = 'manage';
    this.clearPendingProducts();
    this.clearManagedProducts();
    this.mockupModalError = '';
    this.mockupModalSuccess = '';
    this.isMockupModalOpen = true;
    this.loadManagedProducts(folder.id);
  }

  openMockupModal(index: number): void {
    this.openCreateProductModal(index);
  }

  closeMockupModal(): void {
    this.isMockupModalOpen = false;
    this.productModalMode = 'create';
    this.activeFolderId = null;
    this.activeFolderName = '';
    this.isLoadingManagedProducts = false;
    this.isSavingManagedProducts = false;
    this.mockupModalError = '';
    this.mockupModalSuccess = '';
    this.clearPendingProducts();
    this.clearManagedProducts();
  }

  onMockupFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];

    this.addMockupFiles(files);
    input.value = '';
  }

  removePendingProduct(index: number): void {
    const product = this.pendingProducts[index];

    if (product) {
      URL.revokeObjectURL(product.previewUrl);
    }

    this.pendingProducts = this.pendingProducts.filter((_, fileIndex) => fileIndex !== index);
  }

  saveProducts(): void {
    if (!this.activeFolderId || !this.activeFolderName) {
      return;
    }

    if (!this.pendingProducts.length) {
      this.mockupModalError = 'Selecione pelo menos uma imagem para cadastrar um produto.';
      return;
    }

    const invalidDraft = this.pendingProducts.find((product) => !product.name.trim());
    if (invalidDraft) {
      this.mockupModalError = 'Todos os produtos precisam ter um nome antes de salvar.';
      return;
    }

    this.isSavingProducts = true;
    this.mockupModalError = '';
    this.mockupModalSuccess = '';

    const requests = this.pendingProducts.map((product) =>
      this.readFileAsDataUrl(product.file).pipe(
        switchMap((image) =>
          this.folderMockup.createMockup(this.activeFolderId!, this.buildProductPayload(product, image))
        )
      )
    );

    (requests.length ? forkJoin(requests) : of([]))
      .pipe(
        finalize(() => {
          this.isSavingProducts = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.mockupModalSuccess = 'Produtos salvos com sucesso.';
          this.clearPendingProducts();
        },
        error: () => {
          this.mockupModalError = 'Nao foi possivel salvar os produtos agora.';
        },
      });
  }

  saveManagedProducts(): void {
    if (!this.activeFolderId || !this.activeFolderName) {
      return;
    }

    if (!this.managedProducts.length) {
      this.mockupModalError = 'Nenhum produto cadastrado nesta pasta para editar.';
      return;
    }

    const invalidProduct = this.managedProducts.find((product) => !product.name.trim());
    if (invalidProduct) {
      this.mockupModalError = 'Todos os produtos precisam ter um nome antes de salvar.';
      return;
    }

    this.isSavingManagedProducts = true;
    this.mockupModalError = '';
    this.mockupModalSuccess = '';

    const requests = this.managedProducts.map((product) =>
      this.folderMockup.updateMockup(
        this.activeFolderId!,
        product.id,
        this.buildManagedProductPayload(product),
      ),
    );

    (requests.length ? forkJoin(requests) : of([]))
      .pipe(
        finalize(() => {
          this.isSavingManagedProducts = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: (products) => {
          this.managedProducts = products.map((product) => this.mapManagedProduct(product));
          this.mockupModalSuccess = 'Produtos atualizados com sucesso.';
        },
        error: () => {
          this.mockupModalError = 'Nao foi possivel salvar as alteracoes agora.';
        },
      });
  }

  removeManagedProduct(productId: number): void {
    if (!this.activeFolderId) {
      return;
    }

    const product = this.managedProducts.find((existingProduct) => existingProduct.id === productId);

    if (!product || product.isDeleting) {
      return;
    }

    const shouldRemove = window.confirm(`Remover "${product.name}" desta pasta?`);

    if (!shouldRemove) {
      return;
    }

    this.mockupModalError = '';
    this.mockupModalSuccess = '';
    product.isDeleting = true;

    this.folderMockup
      .deleteMockup(this.activeFolderId, productId)
      .pipe(
        finalize(() => {
          product.isDeleting = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.managedProducts = this.managedProducts.filter(
            (existingProduct) => existingProduct.id !== productId,
          );
          this.mockupModalSuccess = 'Produto removido com sucesso.';
        },
        error: () => {
          this.mockupModalError = 'Nao foi possivel remover o produto agora.';
        },
      });
  }

  addFolderOption(): void {
    const folderName = window.prompt('Nome do novo tipo de pasta:');
    const trimmedFolderName = folderName?.trim();

    if (!trimmedFolderName) {
      return;
    }

    this.folderActionError = '';

    this.folderMockup.createFolder({ name: trimmedFolderName }).subscribe({
      next: (folder) => {
        this.folders = [...this.folders, folder];
        this.selectedFolderIndex = this.folders.findIndex(
          (existingFolder) => existingFolder.id === folder.id,
        );
        this.changeDetectorRef.detectChanges();
      },
      error: () => {
        this.folderActionError = 'Nao foi possivel criar a pasta agora.';
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  editFolderOption(): void {
    const currentFolder = this.selectedFolder;

    if (!currentFolder) {
      return;
    }

    const folderName = window.prompt('Editar tipo de pasta:', currentFolder.name);
    const trimmedFolderName = folderName?.trim();

    if (!trimmedFolderName) {
      return;
    }

    this.folderActionError = '';

    this.folderMockup.updateFolder(currentFolder.id, { name: trimmedFolderName }).subscribe({
      next: (updatedFolder) => {
        this.folders = this.folders.map((folder, index) =>
          index === this.selectedFolderIndex ? updatedFolder : folder,
        );
        this.activeFolderName =
          this.activeFolderId === updatedFolder.id ? updatedFolder.name : this.activeFolderName;
        this.changeDetectorRef.detectChanges();
      },
      error: () => {
        this.folderActionError = 'Nao foi possivel editar a pasta agora.';
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  removeFolderOption(): void {
    const currentFolder = this.selectedFolder;

    if (!currentFolder) {
      return;
    }

    const shouldRemove = window.confirm(`Remover "${currentFolder.name}" do submenu?`);

    if (!shouldRemove) {
      return;
    }

    this.folderActionError = '';

    this.folderMockup.deleteFolder(currentFolder.id).subscribe({
      next: () => {
        this.folders = this.folders.filter((folder) => folder.id !== currentFolder.id);
        this.selectedFolderIndex = Math.max(0, this.selectedFolderIndex - 1);
        this.changeDetectorRef.detectChanges();
      },
      error: () => {
        this.folderActionError = 'Nao foi possivel remover a pasta agora.';
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  private loadFolders(): void {
    this.isLoadingFolders = true;
    this.folderActionError = '';

    this.folderMockup
      .findAllFolders()
      .pipe(
        finalize(() => {
          this.isLoadingFolders = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: (folders) => {
          this.folders = folders;
          this.selectedFolderIndex = folders.length
            ? Math.min(this.selectedFolderIndex, folders.length - 1)
            : 0;
        },
        error: () => {
          this.folderActionError = 'Nao foi possivel carregar as pastas do sistema.';
        },
      });
  }

  private prepareFolderContext(index: number): FolderResponse | null {
    const folder = this.folders[index];

    if (!folder) {
      return null;
    }

    this.selectedFolderIndex = index;
    this.activeFolderId = folder.id;
    this.activeFolderName = folder.name;

    return folder;
  }

  private loadManagedProducts(folderId: number): void {
    this.isLoadingManagedProducts = true;
    this.mockupModalError = '';
    this.mockupModalSuccess = '';

    this.folderMockup
      .findMockupsByFolderId(folderId)
      .pipe(
        finalize(() => {
          this.isLoadingManagedProducts = false;
          this.changeDetectorRef.detectChanges();
        }),
      )
      .subscribe({
        next: (products) => {
          this.managedProducts = products.map((product) => this.mapManagedProduct(product));
        },
        error: () => {
          this.managedProducts = [];
          this.mockupModalError = 'Nao foi possivel carregar os produtos desta pasta.';
        },
      });
  }

  private addMockupFiles(files: File[]): void {
    const imageFiles = files.filter((file) => file.type.startsWith('image/'));

    if (!imageFiles.length) {
      return;
    }

    const existingKeys = new Set(
      this.pendingProducts.map(
        ({ file }) => `${file.name}-${file.size}-${file.lastModified}`,
      ),
    );

    const freshFiles = imageFiles.filter((file) => {
      const key = `${file.name}-${file.size}-${file.lastModified}`;
      return !existingKeys.has(key);
    });

    this.pendingProducts = [
      ...this.pendingProducts,
      ...freshFiles.map((file) => ({
        file,
        previewUrl: URL.createObjectURL(file),
        name: this.buildDefaultProductName(file.name),
        description: '',
        sku: '',
        available: true,
        stockQuantity: 0,
      })),
    ];
  }

  private clearPendingProducts(): void {
    for (const pendingProduct of this.pendingProducts) {
      URL.revokeObjectURL(pendingProduct.previewUrl);
    }

    this.pendingProducts = [];
  }

  private clearManagedProducts(): void {
    this.managedProducts = [];
  }

  private readFileAsDataUrl(file: File) {
    return of(file).pipe(
      switchMap(
        (selectedFile) =>
          new Promise<string>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result));
            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(selectedFile);
          }),
      ),
    );
  }

  private buildDefaultProductName(fileName: string): string {
    const sanitized = fileName
      .replace(/\.[^.]+$/, '')
      .replace(/[-_]+/g, ' ')
      .trim();
    return sanitized.slice(0, 80) || 'Produto';
  }

  private buildProductPayload(product: PendingProductDraft, image: string): MockupRequest {
    return {
      image,
      name: product.name.trim(),
      type: product.name.trim(),
      description: product.description.trim() || undefined,
      sku: product.sku.trim() || undefined,
      available: product.available,
      stockQuantity: Math.max(0, Number(product.stockQuantity) || 0),
    };
  }

  private buildManagedProductPayload(product: ManagedProductDraft): MockupRequest {
    return {
      image: product.image,
      name: product.name.trim(),
      type: product.name.trim(),
      description: product.description.trim() || undefined,
      sku: product.sku.trim() || undefined,
      available: product.available,
      stockQuantity: Math.max(0, Number(product.stockQuantity) || 0),
    };
  }

  private mapManagedProduct(product: MockupResponse): ManagedProductDraft {
    return {
      id: product.id,
      image: product.image,
      name: (product.name || product.type || 'Produto').trim(),
      description: product.description ?? '',
      sku: product.sku ?? '',
      available: product.available,
      stockQuantity: Math.max(0, Number(product.stockQuantity) || 0),
      isDeleting: false,
    };
  }

  private updateChatBotStatus(isActive: boolean): void {
    this.isChatBotActive = isActive;
    this.isLoadingChatBot = false;
    this.chatBotStatusLabel = isActive ? 'Ativo' : 'Inativo';
    this.changeDetectorRef.detectChanges();
  }

  private ensureElevenLabsWidgetScript(): void {
    if (typeof document === 'undefined') {
      return;
    }

    const existingScript = document.getElementById(Sidebar.elevenLabsScriptId);

    if (existingScript) {
      return;
    }

    const script = document.createElement('script');
    script.id = Sidebar.elevenLabsScriptId;
    script.src = Sidebar.elevenLabsScriptSrc;
    script.async = true;
    script.type = 'text/javascript';
    document.body.appendChild(script);
  }
}
